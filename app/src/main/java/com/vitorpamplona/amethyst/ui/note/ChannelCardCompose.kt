package com.vitorpamplona.amethyst.ui.note

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.BottomStart
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import coil.compose.AsyncImage
import com.vitorpamplona.amethyst.model.LocalCache
import com.vitorpamplona.amethyst.model.Note
import com.vitorpamplona.amethyst.model.User
import com.vitorpamplona.amethyst.service.OnlineChecker
import com.vitorpamplona.amethyst.service.model.LiveActivitiesEvent
import com.vitorpamplona.amethyst.service.model.LiveActivitiesEvent.Companion.STATUS_ENDED
import com.vitorpamplona.amethyst.service.model.LiveActivitiesEvent.Companion.STATUS_LIVE
import com.vitorpamplona.amethyst.service.model.LiveActivitiesEvent.Companion.STATUS_PLANNED
import com.vitorpamplona.amethyst.ui.screen.equalImmutableLists
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.ChannelHeader
import com.vitorpamplona.amethyst.ui.screen.loggedIn.EndedFlag
import com.vitorpamplona.amethyst.ui.screen.loggedIn.LiveFlag
import com.vitorpamplona.amethyst.ui.screen.loggedIn.OfflineFlag
import com.vitorpamplona.amethyst.ui.screen.loggedIn.ScheduledFlag
import com.vitorpamplona.amethyst.ui.theme.QuoteBorder
import com.vitorpamplona.amethyst.ui.theme.Size35dp
import com.vitorpamplona.amethyst.ui.theme.newItemBackgroundColor
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCardCompose(
    baseNote: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val isBlank by baseNote.live().metadata.map {
        it.note.event == null
    }.distinctUntilChanged().observeAsState(baseNote.event == null)

    Crossfade(targetState = isBlank) {
        if (it) {
            LongPressToQuickAction(baseNote = baseNote, accountViewModel = accountViewModel) { showPopup ->
                BlankNote(
                    remember {
                        modifier.combinedClickable(
                            onClick = { },
                            onLongClick = showPopup
                        )
                    },
                    false
                )
            }
        } else {
            CheckHiddenChannelCardCompose(
                baseNote,
                routeForLastRead,
                modifier,
                parentBackgroundColor,
                accountViewModel,
                nav
            )
        }
    }
}

@Composable
fun CheckHiddenChannelCardCompose(
    note: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    val isHidden by accountViewModel.accountLiveData.map {
        accountViewModel.isNoteHidden(note)
    }.distinctUntilChanged().observeAsState(accountViewModel.isNoteHidden(note))

    Crossfade(targetState = isHidden) {
        if (!it) {
            LoadedChannelCardCompose(
                note,
                routeForLastRead,
                modifier,
                parentBackgroundColor,
                accountViewModel,
                nav
            )
        }
    }
}

@Composable
fun LoadedChannelCardCompose(
    note: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    var state by remember {
        mutableStateOf(
            NoteComposeReportState(
                isAcceptable = true,
                canPreview = true,
                relevantReports = persistentSetOf()
            )
        )
    }

    val scope = rememberCoroutineScope()

    WatchForReports(note, accountViewModel) { newIsAcceptable, newCanPreview, newRelevantReports ->
        if (newIsAcceptable != state.isAcceptable || newCanPreview != state.canPreview) {
            val newState = NoteComposeReportState(newIsAcceptable, newCanPreview, newRelevantReports)
            scope.launch(Dispatchers.Main) {
                state = newState
            }
        }
    }

    Crossfade(targetState = state) {
        RenderChannelCardReportState(
            it,
            note,
            routeForLastRead,
            modifier,
            parentBackgroundColor,
            accountViewModel,
            nav
        )
    }
}

@Composable
fun RenderChannelCardReportState(
    state: NoteComposeReportState,
    note: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    var showReportedNote by remember { mutableStateOf(false) }

    Crossfade(targetState = !state.isAcceptable && !showReportedNote) { showHiddenNote ->
        if (showHiddenNote) {
            HiddenNote(
                state.relevantReports,
                accountViewModel,
                modifier,
                false,
                nav,
                onClick = { showReportedNote = true }
            )
        } else {
            NormalChannelCard(
                note,
                routeForLastRead,
                modifier,
                parentBackgroundColor,
                accountViewModel,
                nav
            )
        }
    }
}

@Composable
fun NormalChannelCard(
    baseNote: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    LongPressToQuickAction(baseNote = baseNote, accountViewModel = accountViewModel) { showPopup ->
        CheckNewAndRenderChannelCard(
            baseNote,
            routeForLastRead,
            modifier,
            parentBackgroundColor,
            accountViewModel,
            showPopup,
            nav
        )
    }
}

@Composable
private fun CheckNewAndRenderChannelCard(
    baseNote: Note,
    routeForLastRead: String? = null,
    modifier: Modifier = Modifier,
    parentBackgroundColor: MutableState<Color>? = null,
    accountViewModel: AccountViewModel,
    showPopup: () -> Unit,
    nav: (String) -> Unit
) {
    val newItemColor = MaterialTheme.colors.newItemBackgroundColor
    val defaultBackgroundColor = MaterialTheme.colors.background
    val backgroundColor = remember { mutableStateOf<Color>(defaultBackgroundColor) }

    LaunchedEffect(key1 = routeForLastRead, key2 = parentBackgroundColor?.value) {
        launch(Dispatchers.IO) {
            routeForLastRead?.let {
                val lastTime = accountViewModel.account.loadLastRead(it)

                val createdAt = baseNote.createdAt()
                if (createdAt != null) {
                    accountViewModel.account.markAsRead(it, createdAt)

                    val isNew = createdAt > lastTime

                    val newBackgroundColor = if (isNew) {
                        if (parentBackgroundColor != null) {
                            newItemColor.compositeOver(parentBackgroundColor.value)
                        } else {
                            newItemColor.compositeOver(defaultBackgroundColor)
                        }
                    } else {
                        parentBackgroundColor?.value ?: defaultBackgroundColor
                    }

                    if (newBackgroundColor != backgroundColor.value) {
                        launch(Dispatchers.Main) {
                            backgroundColor.value = newBackgroundColor
                        }
                    }
                }
            } ?: run {
                val newBackgroundColor = parentBackgroundColor?.value ?: defaultBackgroundColor

                if (newBackgroundColor != backgroundColor.value) {
                    launch(Dispatchers.Main) {
                        backgroundColor.value = newBackgroundColor
                    }
                }
            }
        }
    }

    ClickableNote(
        baseNote = baseNote,
        backgroundColor = backgroundColor,
        modifier = modifier,
        accountViewModel = accountViewModel,
        showPopup = showPopup,
        nav = nav
    ) {
        InnerChannelCardWithReactions(
            baseNote = baseNote,
            accountViewModel = accountViewModel,
            nav = nav
        )
    }
}

@Composable
fun InnerChannelCardWithReactions(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    Column() {
        RenderNoteRow(
            baseNote,
            accountViewModel,
            nav
        )
    }
}

@Composable
private fun RenderNoteRow(
    baseNote: Note,
    accountViewModel: AccountViewModel,
    nav: (String) -> Unit
) {
    when (remember { baseNote.event }) {
        is LiveActivitiesEvent -> {
            RenderLiveActivityThumb(baseNote, accountViewModel, nav)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RenderLiveActivityThumb(baseNote: Note, accountViewModel: AccountViewModel, nav: (String) -> Unit) {
    val noteEvent = baseNote.event as? LiveActivitiesEvent ?: return

    val eventUpdates by baseNote.live().metadata.observeAsState()

    val media = remember(eventUpdates) { noteEvent.streaming() }
    val cover by remember(eventUpdates) {
        derivedStateOf {
            noteEvent.image()?.ifBlank { null }
        }
    }
    val subject = remember(eventUpdates) { noteEvent.title()?.ifBlank { null } }
    val content = remember(eventUpdates) { noteEvent.summary() }
    val participants = remember(eventUpdates) { noteEvent.participants() }
    val status = remember(eventUpdates) { noteEvent.status() }

    var isOnline by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = media) {
        launch(Dispatchers.IO) {
            val newIsOnline = OnlineChecker.isOnline(media)
            if (isOnline != newIsOnline) {
                isOnline = newIsOnline
            }
        }
    }

    var participantUsers by remember {
        mutableStateOf<ImmutableList<User>>(
            persistentListOf()
        )
    }

    LaunchedEffect(key1 = eventUpdates) {
        launch(Dispatchers.IO) {
            val channel = LocalCache.getChannelIfExists(baseNote.idHex)
            val repliesByFollows = channel?.notes?.mapNotNull { it.value.author?.pubkeyHex }?.toSet() ?: emptySet()

            val zappers = baseNote.publicZapAuthors()
            val likeAuthors = baseNote.reactionAuthors()

            val allParticipants = (repliesByFollows + zappers + likeAuthors).filter { accountViewModel.isFollowing(it) }

            val newParticipantUsers = (
                participants.mapNotNull { part ->
                    if (part.key != baseNote.author?.pubkeyHex) {
                        LocalCache.checkGetOrCreateUser(part.key)
                    } else {
                        null
                    }
                } + allParticipants.mapNotNull {
                    if (it != baseNote.author?.pubkeyHex) {
                        LocalCache.checkGetOrCreateUser(it)
                    } else {
                        null
                    }
                }
                ).toImmutableList()

            if (!equalImmutableLists(newParticipantUsers, participantUsers)) {
                participantUsers = newParticipantUsers
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = TopEnd,
            modifier = Modifier
                .aspectRatio(ratio = 16f / 9f)
                .fillMaxWidth()
        ) {
            cover?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(QuoteBorder)
                )
            } ?: run {
                baseNote.author?.let {
                    DisplayAuthorBanner(it)
                }
            }

            Box(Modifier.padding(10.dp)) {
                Crossfade(targetState = status) {
                    when (it) {
                        STATUS_LIVE -> {
                            if (media.isNullOrBlank()) {
                                LiveFlag()
                            } else if (isOnline) {
                                LiveFlag()
                            } else {
                                OfflineFlag()
                            }
                        }
                        STATUS_ENDED -> {
                            EndedFlag()
                        }
                        STATUS_PLANNED -> {
                            ScheduledFlag()
                        }
                        else -> {
                            EndedFlag()
                        }
                    }
                }
            }

            Box(Modifier.padding(10.dp).align(BottomStart)) {
                if (participantUsers.isNotEmpty()) {
                    FlowRow() {
                        participantUsers.forEach {
                            ClickableUserPicture(it, Size35dp, accountViewModel)
                        }
                    }
                }
            }
        }

        ChannelHeader(
            channelHex = remember { baseNote.idHex },
            showVideo = false,
            showBottomDiviser = false,
            showFlag = false,
            modifier = remember {
                Modifier.padding(start = 0.dp, end = 0.dp, top = 5.dp, bottom = 5.dp)
            },
            accountViewModel = accountViewModel,
            nav = nav
        )
    }
}

@Composable
fun DisplayAuthorBanner(author: User) {
    val picture by author.live().metadata.map {
        it.user.info?.banner?.ifBlank { null } ?: it.user.info?.picture?.ifBlank { null }
    }.observeAsState()

    AsyncImage(
        model = picture,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .clip(QuoteBorder)
    )
}
