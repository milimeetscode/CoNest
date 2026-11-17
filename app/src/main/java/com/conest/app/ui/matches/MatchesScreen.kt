package com.conest.app.ui.matches

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.conest.app.data.social.Match
import com.conest.app.data.social.MatchStatus
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Hairline
import com.conest.app.ui.components.Kicker
import com.conest.app.ui.components.SplitHeadline
import com.conest.app.ui.components.Wordmark
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.NewsreaderDisplay
import com.conest.app.ui.theme.NewsreaderText
import com.conest.app.ui.theme.Oatmeal
import com.conest.app.ui.theme.Terracotta
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MatchesScreen(viewModel: MatchesViewModel, onOpenChat: (String) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().paperTexture(Linen)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = Gutter)) {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 12.dp)) { Wordmark(size = 21f) }
                    Hairline()
                    Spacer(Modifier.height(28.dp))
                    Kicker("Your matches")
                    Spacer(Modifier.height(14.dp))
                    SplitHeadline("People who", "said yes too.", maxSize = 38f)
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (!state.loading && state.fresh.isEmpty() && state.conversations.isEmpty()) {
                item { EmptyMatches(state.failed) }
            }

            if (state.fresh.isNotEmpty()) {
                item {
                    Text(
                        "NEW · SAY HELLO",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedInk,
                        modifier = Modifier.padding(horizontal = Gutter),
                    )
                    Spacer(Modifier.height(12.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = Gutter),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.fresh, key = { it.id }) { m -> FreshMatch(m) { onOpenChat(m.id) } }
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            if (state.conversations.isNotEmpty()) {
                item {
                    Text(
                        "CONVERSATIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedInk,
                        modifier = Modifier.padding(horizontal = Gutter),
                    )
                    Spacer(Modifier.height(8.dp))
                }
                items(state.conversations, key = { it.id }) { m ->
                    ConversationRow(m, state.me) { onOpenChat(m.id) }
                }
            }
        }
    }
}

@Composable
private fun FreshMatch(m: Match, onClick: () -> Unit) {
    Column(
        Modifier
            .width(92.dp)
            .clickable(role = Role.Button, onClickLabel = "Message ${m.other?.name}", onClick = onClick),
    ) {
        AsyncImage(
            model = m.other?.photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(Oatmeal),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            m.other?.name?.substringBefore(' ').orEmpty(),
            fontFamily = NewsreaderDisplay,
            fontSize = 17.sp,
            color = Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ConversationRow(m: Match, me: String, onClick: () -> Unit) {
    val name = m.other?.name ?: "Former member"
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Open conversation with $name", onClick = onClick)
            .padding(horizontal = Gutter),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = m.other?.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).background(Oatmeal, MaterialTheme.shapes.extraSmall),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        fontFamily = NewsreaderDisplay,
                        fontWeight = FontWeight(if (m.unread) 500 else 400),
                        fontSize = 19.sp,
                        color = Ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (m.unread) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(7.dp).background(Terracotta))
                    }
                }
                Spacer(Modifier.height(2.dp))
                val preview = when {
                    m.other == null -> "This account has been deleted"
                    m.status == MatchStatus.Blocked -> "Conversation closed"
                    else -> (if (m.lastSenderId == me) "You: " else "") + m.lastMessage.orEmpty()
                }
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (m.unread) Ink else MutedInk,
                    fontStyle = if (m.status == MatchStatus.Blocked || m.other == null) FontStyle.Italic else FontStyle.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(shortTime(m.sortTime), style = MaterialTheme.typography.labelSmall, color = MutedInk)
        }
        Hairline()
    }
}

@Composable
private fun EmptyMatches(failed: Boolean) {
    Column(Modifier.padding(horizontal = Gutter)) {
        Hairline()
        Spacer(Modifier.height(24.dp))
        Text(
            if (failed) "We couldn't load your matches. Check your connection." else
                "No matches yet. When someone you're interested in feels the same, they'll appear here and you can start talking.",
            fontFamily = NewsreaderText,
            fontStyle = FontStyle.Italic,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            color = MutedInk,
            modifier = Modifier.widthIn(max = 340.dp),
        )
    }
}

/** "10:42", "YESTERDAY", "MON", or "12 NOV". */
internal fun shortTime(date: Date?): String {
    if (date == null) return ""
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { time = date }
    val days = ((startOfDay(now) - startOfDay(then)) / 86_400_000L).toInt()
    return when {
        days <= 0 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        days == 1 -> "YESTERDAY"
        days < 7 -> SimpleDateFormat("EEE", Locale.ENGLISH).format(date).uppercase()
        else -> SimpleDateFormat("d MMM", Locale.ENGLISH).format(date).uppercase()
    }
}

private fun startOfDay(c: Calendar): Long = (c.clone() as Calendar).apply {
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

