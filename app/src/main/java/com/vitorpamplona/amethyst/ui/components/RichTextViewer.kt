package com.vitorpamplona.amethyst.ui.components

import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.flowlayout.FlowRow
import com.halilibo.richtext.markdown.Markdown
import com.halilibo.richtext.markdown.MarkdownParseOptions
import com.halilibo.richtext.ui.RichTextStyle
import com.halilibo.richtext.ui.material.MaterialRichText
import com.halilibo.richtext.ui.resolveDefaults
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.model.checkForHashtagWithIcon
import com.vitorpamplona.amethyst.service.lnurl.LnWithdrawalUtil
import com.vitorpamplona.amethyst.service.nip19.Nip19
import com.vitorpamplona.amethyst.ui.note.NoteCompose
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.regex.Pattern

val imageExtensions = listOf("png", "jpg", "gif", "bmp", "jpeg", "webp", "svg")
val videoExtensions = listOf("mp4", "avi", "wmv", "mpg", "amv", "webm", "mov")

// Group 1 = url, group 4 additional chars
val noProtocolUrlValidator = Pattern.compile("(([\\w\\d-]+\\.)*[a-zA-Z][\\w-]+[\\.\\:]\\w+([\\/\\?\\=\\&\\#\\.]?[\\w-]+)*\\/?)(.*)")

val tagIndex = Pattern.compile("\\#\\[([0-9]+)\\](.*)")

val mentionsPattern: Pattern = Pattern.compile("@([A-Za-z0-9_\\-]+)")
val hashTagsPattern: Pattern = Pattern.compile("#([a-z0-9_\\-]+)(.*)", Pattern.CASE_INSENSITIVE)
val urlPattern: Pattern = Patterns.WEB_URL

fun isValidURL(url: String?): Boolean {
    return try {
        URL(url).toURI()
        true
    } catch (e: MalformedURLException) {
        false
    } catch (e: URISyntaxException) {
        false
    }
}

val richTextDefaults = RichTextStyle().resolveDefaults()

@Composable
fun RichTextViewer(
    content: String,
    canPreview: Boolean,
    modifier: Modifier = Modifier,
    tags: List<List<String>>?,
    backgroundColor: Color,
    accountViewModel: AccountViewModel,
    navController: NavController
) {
    Column(modifier = modifier) {
        if (content.startsWith("# ") ||
            content.contains("##") ||
            content.contains("**") ||
            content.contains("__") ||
            content.contains("```")
        ) {
            val myMarkDownStyle = richTextDefaults.copy(
                codeBlockStyle = richTextDefaults.codeBlockStyle?.copy(
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    modifier = Modifier
                        .padding(0.dp)
                        .fillMaxWidth()
                        .clip(shape = RoundedCornerShape(15.dp))
                        .border(
                            1.dp,
                            MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                            RoundedCornerShape(15.dp)
                        )
                        .background(
                            MaterialTheme.colors.onSurface
                                .copy(alpha = 0.05f)
                                .compositeOver(backgroundColor)
                        )
                ),
                stringStyle = richTextDefaults.stringStyle?.copy(
                    linkStyle = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        color = MaterialTheme.colors.primary
                    ),
                    codeStyle = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        background = MaterialTheme.colors.onSurface.copy(alpha = 0.22f).compositeOver(backgroundColor)
                    )
                )
            )

            MaterialRichText(
                style = myMarkDownStyle
            ) {
                Markdown(
                    content = content,
                    markdownParseOptions = MarkdownParseOptions.Default
                )
            }
        } else {
            val imagesForPager = mutableListOf<String>()

            content.split('\n').forEach { paragraph ->
                paragraph.split(' ').forEach { word: String ->
                    // sequence of images will render in a slideview
                    if (isValidURL(word)) {
                        val removedParamsFromUrl = word.split("?")[0].lowercase()
                        if (imageExtensions.any { removedParamsFromUrl.endsWith(it) }) {
                            imagesForPager.add(word)
                        }
                        if (videoExtensions.any { removedParamsFromUrl.endsWith(it) }) {
                            imagesForPager.add(word)
                        }
                    }
                }
            }

            // FlowRow doesn't work well with paragraphs. So we need to split them
            content.split('\n').forEach { paragraph ->
                FlowRow() {
                    val s = if (isArabic(paragraph)) paragraph.split(' ').reversed() else paragraph.split(' ')
                    s.forEach { word: String ->
                        if (canPreview) {
                            // Explicit URL
                            if (isValidURL(word)) {
                                val removedParamsFromUrl = word.split("?")[0].lowercase()
                                if (imageExtensions.any { removedParamsFromUrl.endsWith(it) }) {
                                    ZoomableImageView(word, imagesForPager)
                                } else if (videoExtensions.any { removedParamsFromUrl.endsWith(it) }) {
                                    ZoomableImageView(word, imagesForPager)
                                } else {
                                    UrlPreview(word, "$word ")
                                }
                            } else if (word.startsWith("lnbc", true)) {
                                MayBeInvoicePreview(word)
                            } else if (word.startsWith("lnurl", true)) {
                                MayBeWithdrawal(word)
                            } else if (Patterns.EMAIL_ADDRESS.matcher(word).matches()) {
                                ClickableEmail(word)
                            } else if (word.length > 6 && Patterns.PHONE.matcher(word).matches()) {
                                ClickablePhone(word)
                            } else if (isBechLink(word)) {
                                BechLink(
                                    word,
                                    canPreview,
                                    backgroundColor,
                                    accountViewModel,
                                    navController
                                )
                            } else if (word.startsWith("#")) {
                                if (tagIndex.matcher(word).matches() && tags != null) {
                                    TagLink(
                                        word,
                                        tags,
                                        canPreview,
                                        backgroundColor,
                                        accountViewModel,
                                        navController
                                    )
                                } else if (hashTagsPattern.matcher(word).matches()) {
                                    HashTag(word, accountViewModel, navController)
                                } else {
                                    Text(
                                        text = "$word ",
                                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Content)
                                    )
                                }
                            } else if (noProtocolUrlValidator.matcher(word).matches()) {
                                val matcher = noProtocolUrlValidator.matcher(word)
                                matcher.find()
                                val url = matcher.group(1) // url
                                val additionalChars = matcher.group(4) ?: "" // additional chars

                                ClickableUrl(url, "https://$url")
                                Text("$additionalChars ")
                            } else {
                                Text(
                                    text = "$word ",
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Content)
                                )
                            }
                        } else {
                            if (isValidURL(word)) {
                                ClickableUrl("$word ", word)
                            } else if (word.startsWith("lnurl", true)) {
                                val lnWithdrawal = LnWithdrawalUtil.findWithdrawal(word)
                                if (lnWithdrawal != null) {
                                    ClickableWithdrawal(withdrawalString = lnWithdrawal)
                                } else {
                                    Text(
                                        text = "$word ",
                                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Content)
                                    )
                                }
                            } else if (Patterns.EMAIL_ADDRESS.matcher(word).matches()) {
                                ClickableEmail(word)
                            } else if (Patterns.PHONE.matcher(word).matches() && word.length > 6) {
                                ClickablePhone(word)
                            } else if (isBechLink(word)) {
                                BechLink(
                                    word,
                                    canPreview,
                                    backgroundColor,
                                    accountViewModel,
                                    navController
                                )
                            } else if (word.startsWith("#")) {
                                if (tagIndex.matcher(word).matches() && tags != null) {
                                    TagLink(
                                        word,
                                        tags,
                                        canPreview,
                                        backgroundColor,
                                        accountViewModel,
                                        navController
                                    )
                                } else if (hashTagsPattern.matcher(word).matches()) {
                                    HashTag(word, accountViewModel, navController)
                                } else {
                                    Text(
                                        text = "$word ",
                                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Content)
                                    )
                                }
                            } else if (noProtocolUrlValidator.matcher(word).matches()) {
                                val matcher = noProtocolUrlValidator.matcher(word)
                                matcher.find()
                                val url = matcher.group(1) // url
                                val additionalChars = matcher.group(4) ?: "" // additional chars

                                ClickableUrl(url, "https://$url")
                                Text("$additionalChars ")
                            } else {
                                Text(
                                    text = "$word ",
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Content)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun isArabic(text: String): Boolean {
    return text.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }
}

fun isBechLink(word: String): Boolean {
    val cleaned = word.lowercase().removePrefix("@").removePrefix("nostr:").removePrefix("@")

    return listOf("npub1", "naddr1", "note1", "nprofile1", "nevent1").any { cleaned.startsWith(it) }
}

@Composable
fun BechLink(word: String, canPreview: Boolean, backgroundColor: Color, accountViewModel: AccountViewModel, navController: NavController) {
    var nip19Route by remember { mutableStateOf<Nip19.Return?>(null) }
    var baseNotePair by remember { mutableStateOf<Pair<Note, String?>?>(null) }

    LaunchedEffect(key1 = word) {
        withContext(Dispatchers.IO) {
            Nip19.uriToRoute(word)?.let {
                if (it.type == Nip19.Type.NOTE || it.type == Nip19.Type.EVENT || it.type == Nip19.Type.ADDRESS) {
                    LocalCache.checkGetOrCreateNote(it.hex)?.let { note ->
                        baseNotePair = Pair(note, it.additionalChars)
                    }
                } else {
                    nip19Route = it
                }
            }
        }
    }

    if (canPreview) {
        baseNotePair?.let {
            NoteCompose(
                baseNote = it.first,
                accountViewModel = accountViewModel,
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 0.dp, start = 0.dp, end = 0.dp)
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(15.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                        RoundedCornerShape(15.dp)
                    ),
                parentBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
                    .compositeOver(backgroundColor),
                isQuotedNote = true,
                navController = navController
            )
            Text(
                "${it.second} "
            )
        } ?: nip19Route?.let {
            ClickableRoute(it, navController)
        } ?: Text(text = "$word ")
    } else {
        nip19Route?.let {
            ClickableRoute(it, navController)
        } ?: Text(text = "$word ")
    }
}

@Composable
fun HashTag(word: String, accountViewModel: AccountViewModel, navController: NavController) {
    var tagSuffixPair by remember { mutableStateOf<Pair<String, String?>?>(null) }

    LaunchedEffect(key1 = word) {
        withContext(Dispatchers.IO) {
            val hashtagMatcher = hashTagsPattern.matcher(word)

            val (myTag, mySuffix) = try {
                hashtagMatcher.find()
                Pair(hashtagMatcher.group(1), hashtagMatcher.group(2))
            } catch (e: Exception) {
                Log.e("Hashtag Parser", "Couldn't link hashtag $word", e)
                Pair(null, null)
            }

            if (myTag != null) {
                tagSuffixPair = Pair(myTag, mySuffix)
            }
        }
    }

    tagSuffixPair?.let { tagPair ->
        val hashtagIcon = checkForHashtagWithIcon(tagPair.first)
        ClickableText(
            text = buildAnnotatedString {
                withStyle(
                    LocalTextStyle.current.copy(color = MaterialTheme.colors.primary).toSpanStyle()
                ) {
                    append("#${tagPair.first}")
                }
            },
            onClick = { navController.navigate("Hashtag/${tagPair.first}") }
        )

        if (hashtagIcon != null) {
            val myId = "inlineContent"
            val emptytext = buildAnnotatedString {
                withStyle(
                    LocalTextStyle.current.copy(color = MaterialTheme.colors.primary).toSpanStyle()
                ) {
                    append("")
                    appendInlineContent(myId, "[icon]")
                }
            }
            val inlineContent = mapOf(
                Pair(
                    myId,
                    InlineTextContent(
                        Placeholder(
                            width = 17.sp,
                            height = 17.sp,
                            placeholderVerticalAlign = PlaceholderVerticalAlign.Center
                        )
                    ) {
                        Icon(
                            painter = painterResource(hashtagIcon.icon),
                            contentDescription = hashtagIcon.description,
                            tint = hashtagIcon.color,
                            modifier = hashtagIcon.modifier
                        )
                    }
                )
            )

            // Empty Text for Size of Icon
            Text(
                text = emptytext,
                inlineContent = inlineContent
            )
        }
        tagPair.second?.ifBlank { "" }?.let {
            Text(text = "$it ")
        }
    } ?: Text(text = "$word ")
}

@Composable
fun TagLink(word: String, tags: List<List<String>>, canPreview: Boolean, backgroundColor: Color, accountViewModel: AccountViewModel, navController: NavController) {
    var baseUserPair by remember { mutableStateOf<Pair<User, String?>?>(null) }
    var baseNotePair by remember { mutableStateOf<Pair<Note, String?>?>(null) }

    LaunchedEffect(key1 = word) {
        withContext(Dispatchers.IO) {
            val matcher = tagIndex.matcher(word)
            val (index, suffix) = try {
                matcher.find()
                Pair(matcher.group(1)?.toInt(), matcher.group(2) ?: "")
            } catch (e: Exception) {
                Log.w("Tag Parser", "Couldn't link tag $word", e)
                Pair(null, null)
            }

            if (index != null && index >= 0 && index < tags.size) {
                val tag = tags[index]

                if (tag.size > 1) {
                    if (tag[0] == "p") {
                        LocalCache.checkGetOrCreateUser(tag[1])?.let {
                            baseUserPair = Pair(it, suffix)
                        }
                    } else if (tag[0] == "e" || tag[0] == "a") {
                        LocalCache.checkGetOrCreateNote(tag[1])?.let {
                            baseNotePair = Pair(it, suffix)
                        }
                    }
                }
            }
        }
    }

    baseUserPair?.let {
        ClickableUserTag(it.first, navController)
        Text(text = "${it.second} ")
    }

    baseNotePair?.let {
        if (canPreview) {
            NoteCompose(
                baseNote = it.first,
                accountViewModel = accountViewModel,
                modifier = Modifier
                    .padding(top = 2.dp, bottom = 0.dp, start = 0.dp, end = 0.dp)
                    .fillMaxWidth()
                    .clip(shape = RoundedCornerShape(15.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                        RoundedCornerShape(15.dp)
                    ),
                parentBackgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.05f)
                    .compositeOver(backgroundColor),
                isQuotedNote = true,
                navController = navController
            )
            it.second?.ifBlank { null }?.let {
                Text(text = "$it ")
            }
        } else {
            ClickableNoteTag(it.first, navController)
            Text(text = "${it.second} ")
        }
    }

    if (baseNotePair == null && baseUserPair == null) {
        Text(text = "$word ")
    }
}
