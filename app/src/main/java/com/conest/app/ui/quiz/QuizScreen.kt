package com.conest.app.ui.quiz

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.conest.app.ui.components.ChoiceCard
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Kicker
import com.conest.app.ui.components.LineArrow
import com.conest.app.ui.components.PrimaryAction
import com.conest.app.ui.components.SplitHeadline
import com.conest.app.ui.components.StepProgress
import com.conest.app.ui.components.TextLink
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.NewsreaderText
import com.conest.app.ui.theme.Terracotta
import kotlinx.coroutines.delay

/**
 * The lifestyle questionnaire: one question per page, conversational, auto-advancing after a
 * choice so it feels like answering rather than filling in a form.
 */
@Composable
fun QuizScreen(
    onFinished: () -> Unit,
    onExit: () -> Unit,
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var started by rememberSaveable { mutableStateOf(state.editing) }
    var advanceTick by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(advanceTick) {
        if (advanceTick > 0) {
            delay(380) // long enough to see the choice land
            viewModel.next()
        }
    }

    val goBack: () -> Unit = {
        when {
            started && viewModel.back() -> Unit
            started && !state.editing -> started = false
            else -> onExit()
        }
    }
    BackHandler(onBack = goBack)

    Box(
        Modifier
            .fillMaxSize()
            .paperTexture(Linen),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = Gutter, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clickable(role = Role.Button, onClickLabel = "Back", onClick = goBack)
                        .semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center,
                ) {
                    LineArrow(Modifier.size(width = 22.dp, height = 12.dp), color = Ink, pointsLeft = true)
                }
                Spacer(Modifier.weight(1f))
                if (started) {
                    Text(
                        "QUESTION %02d / %02d".format(state.index + 1, state.total),
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedInk,
                    )
                }
            }
            if (started) {
                StepProgress(state.index + 1, state.total, Modifier.padding(horizontal = Gutter, vertical = 10.dp))
            }

            AnimatedContent(
                targetState = if (started) state.index else -1,
                transitionSpec = {
                    val forward = targetState > initialState
                    (fadeIn(tween(260, 80)) + slideInHorizontally(tween(320)) { (if (forward) 1 else -1) * it / 12 }) togetherWith fadeOut(tween(120))
                },
                label = "question",
                modifier = Modifier.weight(1f),
            ) { index ->
                if (index < 0) {
                    QuizIntro(onBegin = { started = true })
                } else {
                    QuestionPage(
                        state = state,
                        onSelect = { option ->
                            viewModel.select(option)
                            if (!state.isLast) advanceTick++
                        },
                        onNext = viewModel::next,
                        onFinish = { viewModel.finish(onFinished) },
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizIntro(onBegin: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = Gutter),
    ) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            // Oversized italic numeral: the count, set like a magazine section opener.
            Text(
                "10",
                fontFamily = com.conest.app.ui.theme.NewsreaderDisplay,
                fontStyle = FontStyle.Italic,
                fontWeight = androidx.compose.ui.text.font.FontWeight(300),
                fontSize = 220.sp,
                lineHeight = 220.sp,
                color = com.conest.app.ui.theme.Straw,
            )
        }
        Kicker("Lifestyle · about 2 minutes")
        Spacer(Modifier.height(18.dp))
        SplitHeadline("Now, how you", "actually live.", maxSize = 44f)
        Spacer(Modifier.height(16.dp))
        Text(
            "Ten quick questions. There are no right answers, only honest ones. They're how we work out who you'd get on with at home.",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedInk,
            modifier = Modifier.widthIn(max = 340.dp),
        )
        Spacer(Modifier.height(36.dp))
        PrimaryAction("Begin", onBegin)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun QuestionPage(
    state: QuizUiState,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit,
) {
    val q = state.question
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Gutter),
    ) {
        Spacer(Modifier.height(30.dp))
        Kicker(q.kicker)
        Spacer(Modifier.height(16.dp))
        Text(q.prompt, style = MaterialTheme.typography.displayMedium.copy(fontSize = 34.sp, lineHeight = 38.sp), color = Ink)
        Spacer(Modifier.height(12.dp))
        Text(
            q.why,
            fontFamily = NewsreaderText,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = MutedInk,
            modifier = Modifier.widthIn(max = 330.dp),
        )
        Spacer(Modifier.height(28.dp))

        q.options.forEachIndexed { i, option ->
            ChoiceCard(
                marker = "${'a' + i}.",
                title = option.title,
                detail = option.detail,
                selected = state.selected == i,
                onClick = { onSelect(i) },
                enabled = !state.saving,
            )
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(16.dp))
        if (state.isLast) {
            AnimatedVisibility(state.selected != null) {
                Column {
                    if (state.error != null) {
                        Text(state.error, style = MaterialTheme.typography.bodyMedium, color = Terracotta)
                        Spacer(Modifier.height(12.dp))
                    }
                    PrimaryAction(
                        text = if (state.editing) "Save answers" else "Finish",
                        loading = state.saving,
                        loadingText = "Saving your answers",
                        onClick = onFinish,
                    )
                }
            }
        } else if (state.selected != null) {
            // Revisiting an answered question: let people move on without re-choosing.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextLink("", "Next question", onNext)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
