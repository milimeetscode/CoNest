package com.conest.app.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.conest.app.data.model.SetupStatus
import com.conest.app.data.model.setupStatus
import com.conest.app.ui.auth.AuthMode
import com.conest.app.ui.auth.AuthScreen
import com.conest.app.ui.auth.AuthViewModel
import com.conest.app.ui.chat.ChatScreen
import com.conest.app.ui.components.MainTab
import com.conest.app.ui.components.TabBar
import com.conest.app.ui.discover.DiscoverScreen
import com.conest.app.ui.discover.DiscoverViewModel
import com.conest.app.ui.home.HomeScreen
import com.conest.app.ui.matches.MatchesScreen
import com.conest.app.ui.matches.MatchesViewModel
import com.conest.app.ui.onboarding.OnboardingScreen
import com.conest.app.ui.person.PersonContext
import com.conest.app.ui.person.PersonScreen
import com.conest.app.ui.quiz.QuizScreen
import com.conest.app.ui.root.LoadingScreen
import com.conest.app.ui.root.NotConfiguredScreen
import com.conest.app.ui.root.ProfileUnavailableScreen
import com.conest.app.ui.root.RootState
import com.conest.app.ui.root.RootViewModel
import com.conest.app.ui.root.SetupDoneScreen
import com.conest.app.ui.settings.SettingsScreen
import com.conest.app.ui.welcome.WelcomeScreen

object Routes {
    const val Loading = "loading"
    const val NotConfigured = "not-configured"
    const val Unavailable = "unavailable"
    const val Welcome = "welcome"
    const val Auth = "auth/{mode}"
    fun auth(mode: AuthMode) = "auth/${mode.name}"
    const val Onboarding = "onboarding?mode={mode}"
    fun onboarding(edit: Boolean) = "onboarding?mode=${if (edit) "edit" else "setup"}"
    const val Quiz = "quiz?mode={mode}"
    fun quiz(edit: Boolean) = "quiz?mode=${if (edit) "edit" else "setup"}"
    const val Done = "done"

    // Main tabs
    const val Discover = "discover"
    const val Matches = "matches"
    const val Profile = "profile"

    const val Person = "person/{uid}?ctx={ctx}"
    fun person(uid: String, ctx: PersonContext) = "person/$uid?ctx=${ctx.name}"
    const val Chat = "chat/{matchId}"
    fun chat(matchId: String) = "chat/$matchId"
    const val Settings = "settings"

    val tabs = mapOf(Discover to MainTab.Discover, Matches to MainTab.Matches, Profile to MainTab.Profile)
}

/**
 * After sign-in the root decides once per session where to start:
 *  - setup complete → Discover (with Matches and Profile alongside)
 *  - new account → Onboarding → Quiz → Done → Discover
 */
@Composable
fun CoNestNavHost(
    nav: NavHostController = rememberNavController(),
    root: RootViewModel = hiltViewModel(),
) {
    val state by root.state.collectAsStateWithLifecycle()

    // Re-route only when *who* is signed in changes, not on every profile update.
    val gate = when (val s = state) {
        RootState.Loading -> null
        RootState.NotConfigured -> "nc"
        RootState.SignedOut -> "out"
        is RootState.SignedIn -> "in:${s.user.uid}"
        is RootState.ProfileUnavailable -> "err:${s.user.uid}"
    }
    LaunchedEffect(gate) {
        val target = when (val s = state) {
            RootState.Loading -> return@LaunchedEffect
            RootState.NotConfigured -> Routes.NotConfigured
            RootState.SignedOut -> Routes.Welcome
            is RootState.ProfileUnavailable -> Routes.Unavailable
            is RootState.SignedIn -> when (s.profile.setupStatus) {
                SetupStatus.NeedsProfile -> Routes.onboarding(edit = false)
                SetupStatus.NeedsQuiz -> Routes.quiz(edit = false)
                SetupStatus.Complete -> Routes.Discover
            }
        }
        nav.navigate(target) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentTab = Routes.tabs[backStack?.destination?.route]
    val signedIn = state is RootState.SignedIn

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            NavHost(
                navController = nav,
                startDestination = Routes.Loading,
                enterTransition = { fadeIn(tween(300, 60)) + slideInHorizontally(tween(360)) { it / 14 } },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(300, 60)) + slideInHorizontally(tween(360)) { -it / 14 } },
                popExitTransition = { fadeOut(tween(160)) },
            ) {
                composable(Routes.Loading) { LoadingScreen() }
                composable(Routes.NotConfigured) { NotConfiguredScreen() }
                composable(Routes.Unavailable) { ProfileUnavailableScreen(onRetry = root::retry, onSignOut = root::signOut) }

                composable(Routes.Welcome) {
                    WelcomeScreen(
                        onGetStarted = { nav.navigate(Routes.auth(AuthMode.SignUp)) },
                        onLogIn = { nav.navigate(Routes.auth(AuthMode.LogIn)) },
                    )
                }

                composable(Routes.Auth, arguments = listOf(navArgument("mode") { type = NavType.StringType })) { entry ->
                    val mode = AuthMode.valueOf(entry.arguments?.getString("mode") ?: AuthMode.SignUp.name)
                    // Shared across the sign-up/log-in switch so the typed email carries over. Resolved
                    // once: after sign-in the stack is cleared while this screen is still animating out.
                    val owner = remember(entry) { runCatching { nav.getBackStackEntry(Routes.Welcome) }.getOrDefault(entry) }
                    val vm: AuthViewModel = hiltViewModel(owner)
                    val ui by vm.state.collectAsStateWithLifecycle()
                    val context = LocalContext.current
                    AuthScreen(
                        mode = mode,
                        email = ui.email,
                        onEmailChange = vm::onEmailChange,
                        onSubmit = { email, password -> vm.submit(mode, email, password) },
                        onGoogle = { vm.google(context) },
                        onSwitchMode = {
                            vm.clearMessages()
                            val other = if (mode == AuthMode.SignUp) AuthMode.LogIn else AuthMode.SignUp
                            nav.navigate(Routes.auth(other)) { popUpTo(Routes.Welcome) }
                        },
                        onBack = { vm.clearMessages(); nav.popBackStack() },
                        onForgotPassword = vm::resetPassword,
                        isSubmitting = ui.isSubmitting,
                        errorMessage = ui.error,
                        notice = ui.notice,
                    )
                }

                composable(Routes.Onboarding, arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "setup" })) { entry ->
                    val editing = entry.arguments?.getString("mode") == "edit"
                    OnboardingScreen(
                        onProfileSaved = {
                            if (editing) nav.popBackStack()
                            else nav.navigate(Routes.quiz(edit = false)) { popUpTo(0) { inclusive = true } }
                        },
                        // Backing out of the first step: leave editing, or "not now" during setup.
                        onExit = { if (editing) nav.popBackStack() else root.signOut() },
                    )
                }

                composable(Routes.Quiz, arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "setup" })) { entry ->
                    val editing = entry.arguments?.getString("mode") == "edit"
                    QuizScreen(
                        onFinished = {
                            if (editing) nav.popBackStack()
                            else nav.navigate(Routes.Done) { popUpTo(0) { inclusive = true } }
                        },
                        onExit = { if (editing) nav.popBackStack() else root.signOut() },
                    )
                }

                composable(Routes.Done) {
                    SetupDoneScreen(onContinue = { nav.navigate(Routes.Discover) { popUpTo(0) { inclusive = true } } })
                }

                // ---- Main tabs ----------------------------------------------------------------

                composable(Routes.Discover, enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(120)) }) {
                    val vm: DiscoverViewModel = hiltViewModel()
                    DiscoverScreen(
                        viewModel = vm,
                        onOpenProfile = { uid -> nav.navigate(Routes.person(uid, PersonContext.Discover)) },
                        onOpenChat = { id -> nav.navigate(Routes.chat(id)) },
                    )
                }

                composable(Routes.Matches, enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(120)) }) {
                    MatchesScreen(viewModel = hiltViewModel<MatchesViewModel>(), onOpenChat = { id -> nav.navigate(Routes.chat(id)) })
                }

                composable(Routes.Profile, enterTransition = { fadeIn(tween(220)) }, exitTransition = { fadeOut(tween(120)) }) {
                    HomeScreen(
                        onRetakeQuiz = { nav.navigate(Routes.quiz(edit = true)) },
                        onEditProfile = { nav.navigate(Routes.onboarding(edit = true)) },
                        onOpenSettings = { nav.navigate(Routes.Settings) },
                    )
                }

                // ---- Detail screens -----------------------------------------------------------

                composable(
                    Routes.Person,
                    arguments = listOf(
                        navArgument("uid") { type = NavType.StringType },
                        navArgument("ctx") { type = NavType.StringType; defaultValue = PersonContext.Match.name },
                    ),
                ) { entry ->
                    val ctx = PersonContext.valueOf(entry.arguments?.getString("ctx") ?: PersonContext.Match.name)
                    // Likes made from the full profile go through Discover's own state, so the deck
                    // and the match moment stay in step.
                    val discoverOwner = remember(entry) { runCatching { nav.getBackStackEntry(Routes.Discover) }.getOrNull() }
                    val discover: DiscoverViewModel? = discoverOwner?.let { hiltViewModel(it) }
                    PersonScreen(
                        context = ctx,
                        onBack = { nav.popBackStack() },
                        onLike = { uid -> discover?.like(uid); nav.popBackStack() },
                        onPass = { uid -> discover?.pass(uid); nav.popBackStack() },
                        onMessage = { nav.popBackStack() }, // opened from a chat: go back to it
                        onBlocked = { uid ->
                            discover?.remove(uid)
                            // Back to whichever tab this started from (a closed chat has nothing to return to).
                            while (nav.currentDestination?.route !in Routes.tabs.keys && nav.popBackStack()) Unit
                        },
                    )
                }

                composable(Routes.Chat, arguments = listOf(navArgument("matchId") { type = NavType.StringType })) {
                    ChatScreen(
                        onBack = { nav.popBackStack() },
                        onOpenProfile = { uid -> nav.navigate(Routes.person(uid, PersonContext.Match)) },
                    )
                }

                composable(Routes.Settings) { SettingsScreen(onBack = { nav.popBackStack() }) }
            }
        }

        // Unread mark on the Matches tab — this view model lives as long as the activity.
        val matches: MatchesViewModel? = if (signedIn) hiltViewModel() else null
        val unread = matches?.state?.collectAsStateWithLifecycle()?.value?.unreadCount ?: 0

        AnimatedVisibility(
            visible = currentTab != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(tween(100)),
        ) {
            TabBar(
                selected = currentTab ?: MainTab.Discover,
                matchesBadge = unread > 0,
                onSelect = { tab ->
                    val route = Routes.tabs.entries.first { it.value == tab }.key
                    nav.navigate(route) {
                        popUpTo(Routes.Discover) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
