package com.vitorpamplona.amethyst.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.rememberPagerState
import com.vitorpamplona.amethyst.ui.dal.GlobalFeedFilter
import com.vitorpamplona.amethyst.ui.dal.HomeConversationsFeedFilter
import com.vitorpamplona.amethyst.ui.dal.HomeNewThreadFeedFilter
import com.vitorpamplona.amethyst.ui.dal.NotificationFeedFilter
import com.vitorpamplona.amethyst.ui.screen.NostrGlobalFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrHomeFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NostrHomeRepliesFeedViewModel
import com.vitorpamplona.amethyst.ui.screen.NotificationViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.AccountViewModel
import com.vitorpamplona.amethyst.ui.screen.loggedIn.BookmarkListScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.ChannelScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.ChatroomListScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.ChatroomScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.HashtagScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.HiddenUsersScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.HomeScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.LoadRedirectScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.NotificationScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.ProfileScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.SearchScreen
import com.vitorpamplona.amethyst.ui.screen.loggedIn.ThreadScreen

@OptIn(ExperimentalPagerApi::class)
@Composable
fun AppNavigation(
    navController: NavHostController,
    accountViewModel: AccountViewModel,
    nextPage: String? = null
) {
    val homePagerState = rememberPagerState()
    var actionableNextPage by remember { mutableStateOf<String?>(nextPage) }

    // Avoids creating ViewModels for performance reasons (up to 1 second delays)
    val accountState by accountViewModel.accountLiveData.observeAsState()
    val account = accountState?.account ?: return

    HomeNewThreadFeedFilter.account = account
    HomeConversationsFeedFilter.account = account

    val homeFeedViewModel: NostrHomeFeedViewModel = viewModel()
    val repliesFeedViewModel: NostrHomeRepliesFeedViewModel = viewModel()

    GlobalFeedFilter.account = account
    val searchFeedViewModel: NostrGlobalFeedViewModel = viewModel()

    NotificationFeedFilter.account = account
    val notifFeedViewModel: NotificationViewModel = viewModel()

    NavHost(navController, startDestination = Route.Home.route) {
        Route.Search.let { route ->
            composable(route.route, route.arguments, content = {
                val scrollToTop = it.arguments?.getBoolean("scrollToTop") ?: false

                SearchScreen(
                    searchFeedViewModel = searchFeedViewModel,
                    accountViewModel = accountViewModel,
                    navController = navController,
                    scrollToTop = scrollToTop
                )

                // Avoids running scroll to top when back button is pressed
                if (scrollToTop) {
                    it.arguments?.remove("scrollToTop")
                }
            })
        }

        Route.Home.let { route ->
            composable(route.route, route.arguments, content = { it ->
                val scrollToTop = it.arguments?.getBoolean("scrollToTop") ?: false
                val nip47 = it.arguments?.getString("nip47")

                HomeScreen(
                    homeFeedViewModel = homeFeedViewModel,
                    repliesFeedViewModel = repliesFeedViewModel,
                    accountViewModel = accountViewModel,
                    navController = navController,
                    pagerState = homePagerState,
                    scrollToTop = scrollToTop,
                    nip47 = nip47
                )

                // Avoids running scroll to top when back button is pressed
                if (scrollToTop) {
                    it.arguments?.remove("scrollToTop")
                }
                if (nip47 != null) {
                    it.arguments?.remove("nip47")
                }
            })
        }

        Route.Notification.let { route ->
            composable(route.route, route.arguments, content = {
                val scrollToTop = it.arguments?.getBoolean("scrollToTop") ?: false

                NotificationScreen(
                    notifFeedViewModel = notifFeedViewModel,
                    accountViewModel = accountViewModel,
                    navController = navController,
                    scrollToTop = scrollToTop
                )

                // Avoids running scroll to top when back button is pressed
                if (scrollToTop) {
                    it.arguments?.remove("scrollToTop")
                }
            })
        }

        composable(Route.Message.route, content = { ChatroomListScreen(accountViewModel, navController) })
        composable(Route.BlockedUsers.route, content = { HiddenUsersScreen(accountViewModel, navController) })
        composable(Route.Bookmarks.route, content = { BookmarkListScreen(accountViewModel, navController) })

        Route.Profile.let { route ->
            composable(route.route, route.arguments, content = {
                ProfileScreen(
                    userId = it.arguments?.getString("id"),
                    accountViewModel = accountViewModel,
                    navController = navController
                )
            })
        }

        Route.Note.let { route ->
            composable(route.route, route.arguments, content = {
                ThreadScreen(
                    noteId = it.arguments?.getString("id"),
                    accountViewModel = accountViewModel,
                    navController = navController
                )
            })
        }

        Route.Hashtag.let { route ->
            composable(route.route, route.arguments, content = {
                HashtagScreen(
                    tag = it.arguments?.getString("id"),
                    accountViewModel = accountViewModel,
                    navController = navController
                )
            })
        }

        Route.Room.let { route ->
            composable(route.route, route.arguments, content = {
                ChatroomScreen(
                    userId = it.arguments?.getString("id"),
                    accountViewModel = accountViewModel,
                    navController = navController
                )
            })
        }

        Route.Channel.let { route ->
            composable(route.route, route.arguments, content = {
                ChannelScreen(
                    channelId = it.arguments?.getString("id"),
                    accountViewModel = accountViewModel,
                    navController = navController
                )
            })
        }

        Route.Event.let { route ->
            composable(route.route, route.arguments, content = {
                LoadRedirectScreen(
                    eventId = it.arguments?.getString("id"),
                    accountViewModel = accountViewModel,
                    navController = navController
                )
            })
        }
    }

    actionableNextPage?.let {
        LaunchedEffect(it) {
            navController.navigate(it)
        }
        actionableNextPage = null
    }
}
