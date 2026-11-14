package com.conest.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.conest.app.data.model.SetupStatus
import com.conest.app.data.model.setupStatus
import com.conest.app.ui.auth.AuthMode
import com.conest.app.ui.auth.AuthScreen
import com.conest.app.ui.auth.AuthViewModel
import com.conest.app.ui.home.HomeScreen
import com.conest.app.ui.onboarding.OnboardingScreen
import com.conest.app.ui.quiz.QuizScreen
import com.conest.app.ui.root.LoadingScreen
import com.conest.app.ui.root.NotConfiguredScreen
import com.conest.app.ui.root.ProfileUnavailableScreen
import com.conest.app.ui.root.RootState
import com.conest.app.ui.root.RootViewModel
import com.conest.app.ui.root.SetupDoneScreen
import com.conest.app.ui.welcome.WelcomeScreen

object Routes {
    const val Loading = "loading"
    const val NotConfigured = "not-configured"
    const val Unavailable = "unavailable"
    const val Welcome = "welcome"
    const val Auth = "auth/{mode}"
    fun auth(mode: AuthMode) = "auth/${mode.name}"
    const val Onboarding = "onboarding"
    const val Quiz = "quiz?mode={mode}"
    fun quiz(edit: Boolean) = "quiz?mode=${if (edit) "edit" else "setup"}"
    const val Done = "done"
    const val Home = "home"
}

/**
 * Two flows after sign-in, decided once per session by the root:
 *  - returning account, setup complete → profile retrieved → Home
 *  - new account → Onboarding → Quiz → Done → Home
 *    (an account that saved a profile but left mid-quiz resumes at the Quiz)
 */
@Composable
fun CoNestNavHost(
    nav: NavHostController = rememberNavController(),
    root: RootViewModel = hiltViewModel(),
) {
    val state by root.state.collectAsStateWithLifecycle()

    // Re-route only when *who* is signed in changes — not on every profile update, or saving
    // the profile mid-onboarding would yank people out of the flow.
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
                SetupStatus.NeedsProfile -> Routes.Onboarding
                SetupStatus.NeedsQuiz -> Routes.quiz(edit = false)
                SetupStatus.Complete -> Routes.Home
            }
        }
        nav.navigate(target) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

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
            // Shared across the sign-up ↔ log-in switch so the typed email carries over.
            // Resolved once, on first composition: after sign-in the root clears the back stack
            // while this screen is still animating out, and a fresh lookup would then throw.
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

        composable(Routes.Onboarding) {
            OnboardingScreen(
                onProfileSaved = {
                    nav.navigate(Routes.quiz(edit = false)) { popUpTo(Routes.Onboarding) { inclusive = true } }
                },
                onExit = root::signOut, // backing out of the first step = "not now"
            )
        }

        composable(Routes.Quiz, arguments = listOf(navArgument("mode") { type = NavType.StringType; defaultValue = "setup" })) { entry ->
            val editing = entry.arguments?.getString("mode") == "edit"
            QuizScreen(
                onFinished = {
                    if (editing) {
                        nav.popBackStack()
                    } else {
                        nav.navigate(Routes.Done) { popUpTo(0) { inclusive = true } }
                    }
                },
                onExit = { if (editing) nav.popBackStack() else root.signOut() },
            )
        }

        composable(Routes.Done) {
            SetupDoneScreen(onContinue = { nav.navigate(Routes.Home) { popUpTo(0) { inclusive = true } } })
        }

        composable(Routes.Home) {
            HomeScreen(onRetakeQuiz = { nav.navigate(Routes.quiz(edit = true)) })
        }
    }
}
