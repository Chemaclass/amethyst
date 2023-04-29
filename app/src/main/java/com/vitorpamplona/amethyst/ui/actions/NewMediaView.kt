package com.vitorpamplona.amethyst.ui.actions

import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.vitorpamplona.amethyst.R
import com.vitorpamplona.amethyst.ui.components.*
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.TextSpinner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NewMediaView(uri: Uri, onClose: () -> Unit, accountViewModel: AccountViewModel, navController: NavController) {
    val account = accountViewModel.accountLiveData.value?.account ?: return
    val resolver = LocalContext.current.contentResolver
    val postViewModel: NewMediaModel = viewModel()
    val context = LocalContext.current

    val scroolState = rememberScrollState()

    LaunchedEffect(Unit) {
        val mediaType = resolver.getType(uri) ?: ""
        postViewModel.load(account, uri, mediaType)
        delay(100)

        postViewModel.imageUploadingError.collect { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = { onClose() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 10.dp)
                    .fillMaxWidth()
                    .fillMaxHeight().imePadding()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CloseButton(onCancel = {
                        postViewModel.cancel()
                        onClose()
                    })

                    if (postViewModel.isUploadingImage) {
                        LoadingAnimation()
                    }

                    PostButton(
                        onPost = {
                            postViewModel.upload(context) {
                                onClose()
                            }
                        },
                        isActive = postViewModel.canPost()
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(scroolState)
                    ) {
                        ImageVideoPost(postViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ImageVideoPost(postViewModel: NewMediaModel) {
    val scope = rememberCoroutineScope()

    val fileServers = listOf(
        Triple(ServersAvailable.IMGUR_NIP_94, stringResource(id = R.string.upload_server_imgur_nip94), stringResource(id = R.string.upload_server_imgur_nip94_explainer)),
        Triple(ServersAvailable.NOSTRIMG_NIP_94, stringResource(id = R.string.upload_server_nostrimg_nip94), stringResource(id = R.string.upload_server_nostrimg_nip94_explainer)),
        Triple(ServersAvailable.NIP95, stringResource(id = R.string.upload_server_relays_nip95), stringResource(id = R.string.upload_server_relays_nip95_explainer))
    )

    val fileServerOptions = fileServers.map { it.second }
    val fileServerExplainers = fileServers.map { it.third }
    val resolver = LocalContext.current.contentResolver

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .windowInsetsPadding(WindowInsets(0.dp, 0.dp, 0.dp, 0.dp))
    ) {
        if (postViewModel.isImage() == true) {
            AsyncImage(
                model = postViewModel.galleryUri.toString(),
                contentDescription = postViewModel.galleryUri.toString(),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets(0.dp, 0.dp, 0.dp, 0.dp))
            )
        } else if (postViewModel.isVideo() == true && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var bitmap by remember { mutableStateOf<Bitmap?>(null) }

            LaunchedEffect(key1 = postViewModel.galleryUri) {
                scope.launch(Dispatchers.IO) {
                    postViewModel.galleryUri?.let {
                        bitmap = resolver.loadThumbnail(it, Size(1200, 1000), null)
                    }
                }
            }

            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "some useful description",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth()
                )
            }
        } else {
            postViewModel.galleryUri?.let {
                VideoView(it)
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextSpinner(
            label = stringResource(id = R.string.file_server),
            placeholder = fileServers.filter { it.first == postViewModel.defaultServer() }.firstOrNull()?.second ?: fileServers[0].second,
            options = fileServerOptions,
            explainers = fileServerExplainers,
            onSelect = {
                postViewModel.selectedServer = fileServers[it].first
            },
            modifier = Modifier
                .windowInsetsPadding(WindowInsets(0.dp, 0.dp, 0.dp, 0.dp))
                .weight(1f)
        )
    }

    if (postViewModel.selectedServer == ServersAvailable.NOSTRIMG_NIP_94 ||
        postViewModel.selectedServer == ServersAvailable.IMGUR_NIP_94 ||
        postViewModel.selectedServer == ServersAvailable.NIP95
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets(0.dp, 0.dp, 0.dp, 0.dp))
        ) {
            OutlinedTextField(
                label = { Text(text = stringResource(R.string.content_description)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets(0.dp, 0.dp, 0.dp, 0.dp)),
                value = postViewModel.description,
                onValueChange = { postViewModel.description = it },
                placeholder = {
                    Text(
                        text = stringResource(R.string.content_description_example),
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.32f)
                    )
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    capitalization = KeyboardCapitalization.Sentences
                )
            )
        }
    }
}
