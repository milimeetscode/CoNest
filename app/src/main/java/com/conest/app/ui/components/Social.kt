package com.conest.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.conest.app.data.social.ReportReason
import com.conest.app.domain.lifestyle.Compatibility
import com.conest.app.domain.lifestyle.LifestyleCategory
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.NewsreaderDisplay
import com.conest.app.ui.theme.NewsreaderText
import com.conest.app.ui.theme.Straw
import com.conest.app.ui.theme.Terracotta

// ---------------------------------------------------------------------------------------------
// Bottom tabs
// ---------------------------------------------------------------------------------------------

enum class MainTab(val label: String) { Discover("Discover"), Matches("Matches"), Profile("Profile") }

/**
 * Text-only tab bar: small caps labels, the active one in ink with a short terracotta rule
 * above it — a table of contents more than an app chrome.
 */
@Composable
fun TabBar(selected: MainTab, onSelect: (MainTab) -> Unit, matchesBadge: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .background(Linen)
            .navigationBarsPadding(),
    ) {
        Hairline()
        Row(Modifier.fillMaxWidth().height(60.dp)) {
            MainTab.entries.forEach { tab ->
                val active = tab == selected
                val color by animateColorAsState(if (active) Ink else MutedInk, label = "tab")
                Column(
                    Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clickable(role = Role.Tab, onClickLabel = tab.label) { onSelect(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        Modifier
                            .width(18.dp)
                            .height(2.dp)
                            .background(if (active) Terracotta else Linen.copy(alpha = 0f)),
                    )
                    Spacer(Modifier.height(9.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(tab.label.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), color = color)
                        if (tab == MainTab.Matches && matchesBadge) {
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(6.dp).background(Terracotta).semantics { contentDescription = "Unread messages" })
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Compatibility
// ---------------------------------------------------------------------------------------------

/** Plain-English names for the questionnaire categories. */
val LifestyleCategory.displayName: String
    get() = when (this) {
        LifestyleCategory.Cleanliness -> "Tidiness"
        LifestyleCategory.SleepSchedule -> "Sleep"
        LifestyleCategory.SocialHabits -> "Guests"
        LifestyleCategory.Noise -> "Noise"
        LifestyleCategory.WorkRoutine -> "Weekdays"
        LifestyleCategory.SharedResponsibilities -> "Chores"
        LifestyleCategory.Privacy -> "Togetherness"
        LifestyleCategory.Food -> "Kitchen"
        LifestyleCategory.Pets -> "Pets"
        LifestyleCategory.Smoking -> "Smoking"
    }

/**
 * The score as a serif numeral with a small-caps caption. "—" when either quiz is incomplete,
 * because a guessed number would be worse than none.
 */
@Composable
fun CompatibilityScore(compatibility: Compatibility?, modifier: Modifier = Modifier, size: Float = 40f) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                compatibility?.score?.toString() ?: "—",
                fontFamily = NewsreaderDisplay,
                fontWeight = FontWeight(300),
                fontSize = size.sp,
                lineHeight = size.sp,
                color = if (compatibility?.dealBreakers?.isNotEmpty() == true) Terracotta else Forest,
            )
            if (compatibility != null) {
                Text("%", fontFamily = NewsreaderDisplay, fontSize = (size * 0.4f).sp, color = MutedInk, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Text(
            if (compatibility != null) "LIFESTYLE FIT" else "NO SCORE YET",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
            color = MutedInk,
        )
    }
}

/** One hairline bar per category — the breakdown behind the score. */
@Composable
fun CompatibilityBreakdown(compatibility: Compatibility, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        compatibility.categoryScores.forEach { (category, score) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(category.displayName.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedInk, modifier = Modifier.width(112.dp))
                Box(Modifier.weight(1f).height(2.dp).background(Straw)) {
                    Box(Modifier.fillMaxWidth(score / 100f).height(2.dp).background(if (score >= 50) Forest else Terracotta))
                }
                Text("$score", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), color = Ink, modifier = Modifier.width(36.dp).padding(start = 10.dp))
            }
        }
        compatibility.dealBreakers.forEach { c ->
            Spacer(Modifier.height(6.dp))
            Text(
                "Heads up: you're at opposite ends on ${c.displayName.lowercase()}.",
                fontFamily = NewsreaderText,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                color = Terracotta,
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Overflow menu + safety dialogs
// ---------------------------------------------------------------------------------------------

/** "···" button with a plain menu; used on profiles and chats. */
@Composable
fun OverflowMenu(items: List<Pair<String, () -> Unit>>, modifier: Modifier = Modifier) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        Box(
            Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClickLabel = "More options") { open = true }
                .semantics { contentDescription = "More options" },
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(3) { Box(Modifier.size(3.5.dp).background(Ink)) }
            }
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = Linen,
            shape = MaterialTheme.shapes.extraSmall,
        ) {
            items.forEach { (label, action) ->
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.bodyLarge, color = Ink) },
                    onClick = { open = false; action() },
                )
            }
        }
    }
}

/** A quiet, square-cornered dialog on linen — the app's alternative to Material's AlertDialog. */
@Composable
fun PaperDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Linen, MaterialTheme.shapes.small)
                .border(1.dp, Straw, MaterialTheme.shapes.small)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
        ) { content() }
    }
}

enum class SafetyDialog { None, Report, Block }

/** Shows whichever safety dialog is open. Shared by profiles and chats. */
@Composable
fun SafetyDialogHost(
    dialog: SafetyDialog,
    name: String,
    busy: Boolean,
    error: String?,
    onBlock: () -> Unit,
    onReport: (ReportReason, String, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    when (dialog) {
        SafetyDialog.Block -> BlockDialog(name, busy, onBlock, onDismiss)
        SafetyDialog.Report -> ReportDialog(name, busy, error, onReport, onDismiss)
        SafetyDialog.None -> Unit
    }
}

@Composable
fun BlockDialog(name: String, busy: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    PaperDialog(onDismiss) {
        Kicker("Block")
        Spacer(Modifier.height(14.dp))
        Text("Block $name?", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(10.dp))
        Text(
            "You won't see each other in Discover, and any conversation will close. $name won't be told.",
            style = MaterialTheme.typography.bodyLarge,
            color = MutedInk,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryAction("Block", onConfirm, loading = busy, loadingText = "Blocking")
        Spacer(Modifier.height(10.dp))
        OutlineAction("Cancel", onDismiss, enabled = !busy)
    }
}

@Composable
fun ReportDialog(
    name: String,
    busy: Boolean,
    error: String?,
    onSubmit: (reason: ReportReason, details: String, alsoBlock: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by rememberSaveable { mutableStateOf<ReportReason?>(null) }
    var details by rememberSaveable { mutableStateOf("") }
    var alsoBlock by rememberSaveable { mutableStateOf(true) }

    PaperDialog(onDismiss) {
        Kicker("Report")
        Spacer(Modifier.height(14.dp))
        Text("What's wrong with $name's profile?", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Spacer(Modifier.height(8.dp))
        Text("Reports are private. We review every one.", style = MaterialTheme.typography.bodyMedium, color = MutedInk)
        Spacer(Modifier.height(18.dp))
        ReportReason.entries.forEach { r ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp)
                    .clickable(role = Role.RadioButton) { reason = r },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(10.dp)
                        .border(1.dp, if (reason == r) Forest else Straw)
                        .background(if (reason == r) Forest else Linen),
                )
                Spacer(Modifier.width(14.dp))
                Text(r.label, style = MaterialTheme.typography.bodyLarge, color = Ink)
            }
        }
        Spacer(Modifier.height(12.dp))
        UnderlineField(
            label = "Anything else? (optional)",
            value = details,
            onValueChange = { details = it.take(1000) },
            placeholder = "What happened",
            singleLine = false,
            minLines = 2,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(role = Role.Checkbox) { alsoBlock = !alsoBlock },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).border(1.dp, if (alsoBlock) Forest else Straw).background(if (alsoBlock) Forest else Linen))
            Spacer(Modifier.width(14.dp))
            Text("Also block $name", style = MaterialTheme.typography.bodyLarge, color = Ink)
        }
        if (error != null) {
            Spacer(Modifier.height(10.dp))
            Text(error, style = MaterialTheme.typography.bodyMedium, color = Terracotta)
        }
        Spacer(Modifier.height(22.dp))
        PrimaryAction(
            text = "Send report",
            onClick = { reason?.let { onSubmit(it, details, alsoBlock) } },
            loading = busy,
            loadingText = "Sending",
            dimmed = reason == null,
        )
        Spacer(Modifier.height(10.dp))
        OutlineAction("Cancel", onDismiss, enabled = !busy)
    }
}

/** Small-caps text button — for secondary actions in headers and rows. */
@Composable
fun CapsButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: androidx.compose.ui.graphics.Color = Ink) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 4.dp),
    )
}
