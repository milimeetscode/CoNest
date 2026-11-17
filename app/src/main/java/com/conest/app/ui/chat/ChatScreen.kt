package com.conest.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.conest.app.data.social.ChatMessage
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Hairline
import com.conest.app.ui.components.LineArrow
import com.conest.app.ui.components.OverflowMenu
import com.conest.app.ui.components.SafetyDialog
import com.conest.app.ui.components.SafetyDialogHost
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.NewsreaderDisplay
import com.conest.app.ui.theme.NewsreaderText
import com.conest.app.ui.theme.Oatmeal
import com.conest.app.ui.theme.Straw
import com.conest.app.ui.theme.Terracotta
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onOpenProfile: (uid: String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val other = state.other
    val first = other?.name?.substringBefore(' ') ?: "them"
    val list = rememberLazyListState()

    // Keep the newest message in view as the conversation grows.
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) list.animateScrollToItem(state.messages.lastIndex)
    }

    Box(Modifier.fillMaxSize().paperTexture(Linen)) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // Header: back, portrait + name (opens their profile), overflow.
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack).semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center,
                ) { LineArrow(Modifier.size(width = 22.dp, height = 12.dp), color = Ink, pointsLeft = true) }
                Row(
                    Modifier
                        .weight(1f)
                        .clickable(enabled = other != null, role = Role.Button, onClickLabel = "View profile") { state.match?.otherUid?.let(onOpenProfile) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = other?.photoUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(38.dp).background(Oatmeal, MaterialTheme.shapes.extraSmall),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(other?.name ?: "Former member", fontFamily = NewsreaderDisplay, fontSize = 19.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        other?.let {
                            Text(
                                it.preferredLocalities.firstOrNull() ?: it.city,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                                color = MutedInk,
                            )
                        }
                    }
                }
                if (other != null) {
                    OverflowMenu(
                        buildList {
                            add("View profile" to { state.match?.otherUid?.let(onOpenProfile) })
                            add("Report $first" to { viewModel.openDialog(SafetyDialog.Report) })
                            if (state.match?.blockedByMe == true) add("Unblock $first" to { viewModel.unblock() })
                            else if (!state.closed) add("Block $first" to { viewModel.openDialog(SafetyDialog.Block) })
                        },
                    )
                }
            }
            Box(Modifier.padding(horizontal = Gutter)) { Hairline() }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.loaded && state.messages.isEmpty() && !state.closed) {
                    EmptyConversation(first, state.starters, viewModel::useStarter)
                } else {
                    LazyColumn(
                        state = list,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = Gutter, vertical = 16.dp),
                    ) {
                        itemsIndexed(state.messages, key = { _, m -> m.id }) { i, m ->
                            val prev = state.messages.getOrNull(i - 1)
                            val next = state.messages.getOrNull(i + 1)
                            if (prev == null || !sameDay(prev.sentAt, m.sentAt)) DayMark(m.sentAt)
                            Bubble(
                                message = m,
                                mine = m.senderId == state.me,
                                // timestamp under the last message of a run from the same sender
                                showTime = next == null || next.senderId != m.senderId || !sameDay(next.sentAt, m.sentAt),
                                tightTop = prev != null && prev.senderId == m.senderId && sameDay(prev.sentAt, m.sentAt),
                            )
                        }
                    }
                }
            }

            // A report without a block leaves the conversation open: thank them, keep the composer.
            if (state.reported && !state.closed) Notice("Thanks for reporting. We'll review this conversation.")
            when {
                state.closed -> Notice(
                    when {
                        other == null -> "This account has been deleted."
                        state.match?.blockedByMe == true -> "You blocked $first. Unblock from the menu to talk again."
                        else -> "This conversation is closed."
                    },
                )
                else -> Composer(
                    draft = state.draft,
                    error = state.sendError,
                    onChange = viewModel::onDraftChange,
                    onSend = viewModel::send,
                )
            }
        }
    }

    SafetyDialogHost(
        dialog = state.dialog,
        name = first,
        busy = state.busy,
        error = state.safetyError,
        onBlock = viewModel::block,
        onReport = viewModel::report,
        onDismiss = viewModel::closeDialog,
    )
}

@Composable
private fun Bubble(message: ChatMessage, mine: Boolean, showTime: Boolean, tightTop: Boolean) {
    // Square-ish corners, with the corner nearest the speaker squared off.
    val shape = if (mine) RoundedCornerShape(10.dp, 10.dp, 2.dp, 10.dp) else RoundedCornerShape(10.dp, 10.dp, 10.dp, 2.dp)
    Column(
        Modifier.fillMaxWidth().padding(top = if (tightTop) 3.dp else 10.dp),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Text(
            message.text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (mine) Linen else Ink,
            modifier = Modifier
                .widthIn(max = 290.dp)
                .background(if (mine) Forest else Oatmeal.copy(alpha = 0.75f), shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
        if (showTime) {
            Spacer(Modifier.height(4.dp))
            Text(
                if (message.pending) "Sending…" else message.sentAt?.let { SimpleDateFormat("HH:mm", Locale.getDefault()).format(it) }.orEmpty(),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                color = MutedInk,
            )
        }
    }
}

@Composable
private fun DayMark(date: Date?) {
    val label = date?.let {
        val days = daysAgo(it)
        when (days) {
            0 -> "TODAY"
            1 -> "YESTERDAY"
            else -> SimpleDateFormat("EEEE d MMMM", Locale.ENGLISH).format(it).uppercase()
        }
    } ?: "TODAY"
    Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(Straw))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedInk, modifier = Modifier.padding(horizontal = 12.dp))
        Box(Modifier.weight(1f).height(1.dp).background(Straw))
    }
}

@Composable
private fun EmptyConversation(name: String, starters: List<String>, onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = Gutter),
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            "You and $name both said yes.\nWhoever writes first sets the tone.",
            fontFamily = NewsreaderText,
            fontStyle = FontStyle.Italic,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            color = MutedInk,
        )
        Spacer(Modifier.height(16.dp))
        Text("TRY ONE OF THESE", style = MaterialTheme.typography.labelSmall, color = MutedInk)
        Spacer(Modifier.height(10.dp))
        starters.forEach { s ->
            Text(
                s,
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(1.dp, Straw, MaterialTheme.shapes.extraSmall)
                    .clickable(role = Role.Button, onClickLabel = "Use this opener") { onPick(s) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Composer(draft: String, error: String?, onChange: (String) -> Unit, onSend: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(Linen)) {
        Hairline()
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodyMedium, color = Terracotta, modifier = Modifier.padding(horizontal = Gutter, vertical = 6.dp))
        }
        Row(
            Modifier.fillMaxWidth().padding(start = Gutter, end = 8.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = draft,
                onValueChange = onChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                cursorBrush = SolidColor(Forest),
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.weight(1f).heightIn(min = 44.dp).padding(vertical = 10.dp),
                decorationBox = { inner ->
                    Box {
                        if (draft.isEmpty()) Text("Write a message", style = MaterialTheme.typography.bodyLarge, color = MutedInk.copy(alpha = 0.6f))
                        inner()
                    }
                },
            )
            val canSend = draft.isNotBlank()
            Box(
                Modifier
                    .size(48.dp)
                    .background(if (canSend) Forest else Forest.copy(alpha = 0.3f), MaterialTheme.shapes.extraSmall)
                    .clickable(enabled = canSend, role = Role.Button, onClickLabel = "Send", onClick = onSend)
                    .semantics { contentDescription = "Send" },
                contentAlignment = Alignment.Center,
            ) { LineArrow(Modifier.size(width = 20.dp, height = 11.dp), color = Linen) }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Column(Modifier.fillMaxWidth()) {
        Hairline()
        Text(
            text,
            fontFamily = NewsreaderText,
            fontStyle = FontStyle.Italic,
            fontSize = 15.sp,
            color = MutedInk,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 18.dp),
        )
    }
}

private fun daysAgo(date: Date): Int {
    fun start(c: Calendar) = c.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
    val today = start(Calendar.getInstance())
    val then = start(Calendar.getInstance().apply { time = date })
    return ((today - then) / 86_400_000L).toInt()
}

private fun sameDay(a: Date?, b: Date?): Boolean = (a == null && b == null) || (a != null && b != null && daysAgo(a) == daysAgo(b))

