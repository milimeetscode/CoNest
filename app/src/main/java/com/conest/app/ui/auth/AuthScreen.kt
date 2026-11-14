package com.conest.app.ui.auth

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conest.app.R
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Hairline
import com.conest.app.ui.components.LineArrow
import com.conest.app.ui.components.OutlineAction
import com.conest.app.ui.components.PrimaryAction
import com.conest.app.ui.components.TextLink
import com.conest.app.ui.components.UnderlineField
import com.conest.app.ui.components.Wordmark
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.components.rise
import com.conest.app.ui.theme.CoNestTheme
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.Moss
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.Oatmeal
import com.conest.app.ui.theme.Terracotta

enum class AuthMode { SignUp, LogIn }

private const val MIN_PASSWORD = 8

/**
 * Sign up and log in share one quiet page: back + wordmark, an editorial headline, two
 * ruled fields, one solid action, Google as the alternative, and a link to the other mode.
 *
 * Stateless toward the backend: the caller owns [isSubmitting] and [errorMessage] (a future
 * AuthViewModel backed by Firebase Auth). Field state and validation live here.
 */
@Composable
fun AuthScreen(
    mode: AuthMode,
    email: String,
    onEmailChange: (String) -> Unit,
    onSubmit: (email: String, password: String) -> Unit,
    onGoogle: () -> Unit,
    onSwitchMode: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onForgotPassword: () -> Unit = {},
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    notice: String? = null,
    animate: Boolean = true,
) {
    val signUp = mode == AuthMode.SignUp
    val focus = LocalFocusManager.current

    var password by rememberSaveable(mode) { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var attempted by rememberSaveable(mode) { mutableStateOf(false) }

    val emailValid = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val passwordValid = if (signUp) password.length >= MIN_PASSWORD else password.isNotEmpty()

    val emailError = when {
        !attempted -> null
        email.isBlank() -> "We'll need your email."
        !emailValid -> "That doesn't look like an email address."
        else -> null
    }
    val passwordError = when {
        !attempted -> null
        password.isEmpty() -> "Add a password to continue."
        signUp && password.length < MIN_PASSWORD -> "Use at least $MIN_PASSWORD characters."
        else -> null
    }

    fun submit() {
        attempted = true
        if (emailValid && passwordValid && !isSubmitting) {
            focus.clearFocus()
            onSubmit(email.trim(), password)
        }
    }

    var revealed by rememberSaveable(mode) { mutableStateOf(!animate) }
    LaunchedEffect(mode) { revealed = true }

    Box(
        modifier
            .fillMaxSize()
            .paperTexture(Linen),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            TopBar(onBack)

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Gutter),
            ) {
                Spacer(Modifier.height(36.dp))
                Kicker(if (signUp) "Create an account" else "Log in", Modifier.rise(revealed, 0))
                Spacer(Modifier.height(16.dp))
                AuthHeadline(signUp, Modifier.rise(revealed, 120))
                Spacer(Modifier.height(14.dp))
                Text(
                    text = if (signUp) {
                        "It takes a minute. Your profile and a few questions about how you live come next."
                    } else {
                        "Your matches and conversations are right where you left them."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MutedInk,
                    modifier = Modifier
                        .widthIn(max = 330.dp)
                        .rise(revealed, 220),
                )

                Spacer(Modifier.height(36.dp))

                Column(Modifier.rise(revealed, 320)) {
                    ErrorNote(errorMessage)
                    ErrorNote(notice, accent = Moss)

                    UnderlineField(
                        label = "Email",
                        value = email,
                        onValueChange = onEmailChange,
                        placeholder = "you@example.com",
                        error = emailError,
                        enabled = !isSubmitting,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            autoCorrectEnabled = false,
                        ),
                        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                        modifier = Modifier.semantics { contentType = ContentType.EmailAddress },
                    )
                    Spacer(Modifier.height(26.dp))
                    UnderlineField(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = if (signUp) "Choose a password" else "Your password",
                        error = passwordError,
                        enabled = !isSubmitting,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            autoCorrectEnabled = false,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        trailing = {
                            Text(
                                if (showPassword) "HIDE" else "SHOW",
                                style = MaterialTheme.typography.labelSmall,
                                color = Ink,
                                modifier = Modifier
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = if (showPassword) "Hide password" else "Show password",
                                    ) { showPassword = !showPassword }
                                    .padding(start = 12.dp, top = 14.dp, bottom = 14.dp),
                            )
                        },
                        supporting = if (signUp) {
                            { PasswordHint(password.length >= MIN_PASSWORD) }
                        } else null,
                        modifier = Modifier.semantics {
                            contentType = if (signUp) ContentType.NewPassword else ContentType.Password
                        },
                    )

                    if (!signUp) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextLink("", "Forgot password?", onForgotPassword)
                        }
                    } else {
                        Spacer(Modifier.height(28.dp))
                    }

                    PrimaryAction(
                        text = if (signUp) "Create account" else "Log in",
                        loadingText = if (signUp) "Creating your account" else "Logging you in",
                        loading = isSubmitting,
                        onClick = ::submit,
                    )

                    OrDivider(Modifier.padding(vertical = 22.dp))

                    OutlineAction(
                        text = "Continue with Google",
                        onClick = onGoogle,
                        enabled = !isSubmitting,
                        leading = {
                            Image(
                                painter = painterResource(R.drawable.ic_google_g),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )

                    if (signUp) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = buildAnnotatedString {
                                append("By continuing you agree to CoNest's ")
                                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("Terms") }
                                append(" and ")
                                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append("Privacy Policy") }
                                append(".")
                            },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                            color = MutedInk,
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))
            }

            // While typing, the keyboard needs the room — the mode switch steps aside.
            @OptIn(ExperimentalLayoutApi::class)
            val imeOpen = WindowInsets.isImeVisible
            AnimatedVisibility(visible = !imeOpen, enter = fadeIn(), exit = fadeOut(tween(90)) + shrinkVertically(tween(90))) {
            Column(Modifier.padding(horizontal = Gutter)) {
                Hairline()
                TextLink(
                    prompt = if (signUp) "Already have a nest?" else "New to CoNest?",
                    action = if (signUp) "Log in" else "Create an account",
                    onClick = onSwitchMode,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = Gutter, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack)
                .semantics { contentDescription = "Back" },
            contentAlignment = Alignment.Center,
        ) {
            LineArrow(Modifier.size(width = 22.dp, height = 12.dp), color = Ink, pointsLeft = true)
        }
        Wordmark(size = 19f)
    }
}

@Composable
private fun Kicker(text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .width(20.dp)
                .height(1.dp)
                .background(Terracotta),
        )
        Spacer(Modifier.width(10.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedInk)
    }
}

@Composable
private fun AuthHeadline(signUp: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val size = minOf(46f, maxWidth.value / 6.2f)
        val style = MaterialTheme.typography.displayMedium.copy(fontSize = size.sp, lineHeight = (size * 1.02f).sp)
        Column {
            Text(if (signUp) "Let's start" else "Welcome", style = style, color = Ink)
            Text(
                if (signUp) "your nest." else "back home.",
                style = style.copy(fontStyle = FontStyle.Italic),
                color = Terracotta,
            )
        }
    }
}

@Composable
private fun PasswordHint(met: Boolean) {
    val color by animateColorAsState(if (met) Moss else MutedInk, label = "hint")
    Row(verticalAlignment = Alignment.CenterVertically) {
        // a small drawn tick-box: empty rule when unmet, filled moss square when met
        Box(
            Modifier
                .size(7.dp)
                .background(if (met) Moss else Oatmeal),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "At least $MIN_PASSWORD characters",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = color,
        )
    }
}

/** Server-side failures (wrong password, account exists…) — a quiet note, not a red alarm. */
@Composable
private fun ErrorNote(message: String?, accent: androidx.compose.ui.graphics.Color = Terracotta) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .background(Oatmeal.copy(alpha = 0.55f)),
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(accent),
                )
                Text(
                    message.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OrDivider(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Hairline(Modifier.weight(1f))
        Text(
            "or",
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight(400)),
            fontFamily = com.conest.app.ui.theme.NewsreaderText,
            color = MutedInk,
            modifier = Modifier.padding(horizontal = 14.dp),
        )
        Hairline(Modifier.weight(1f))
    }
}

// ---------------------------------------------------------------------------------------------

@Preview(name = "Sign up", widthDp = 393, heightDp = 852)
@Composable
private fun SignUpPreview() {
    CoNestTheme {
        AuthScreen(AuthMode.SignUp, "", {}, { _, _ -> }, {}, {}, {}, animate = false)
    }
}

@Preview(name = "Log in — error", widthDp = 393, heightDp = 852)
@Composable
private fun LogInErrorPreview() {
    CoNestTheme {
        AuthScreen(
            AuthMode.LogIn, "asha@example.com", {}, { _, _ -> }, {}, {}, {},
            errorMessage = "That email and password don't match. Try again, or reset your password.",
            animate = false,
        )
    }
}

@Preview(name = "Log in — submitting", widthDp = 393, heightDp = 852)
@Composable
private fun LogInSubmittingPreview() {
    CoNestTheme {
        AuthScreen(AuthMode.LogIn, "asha@example.com", {}, { _, _ -> }, {}, {}, {}, isSubmitting = true, animate = false)
    }
}
