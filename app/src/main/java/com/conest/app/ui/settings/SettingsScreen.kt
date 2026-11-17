package com.conest.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.conest.app.ui.components.CapsButton
import com.conest.app.ui.components.Gutter
import com.conest.app.ui.components.Hairline
import com.conest.app.ui.components.Kicker
import com.conest.app.ui.components.LineArrow
import com.conest.app.ui.components.OutlineAction
import com.conest.app.ui.components.PrimaryAction
import com.conest.app.ui.components.SplitHeadline
import com.conest.app.ui.components.paperTexture
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.Linen
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.Oatmeal
import com.conest.app.ui.theme.Straw
import com.conest.app.ui.theme.Terracotta

@Composable
fun SettingsScreen(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().paperTexture(Linen)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 6.dp)) {
                Box(
                    Modifier.size(48.dp).clickable(role = Role.Button, onClickLabel = "Back", onClick = onBack).semantics { contentDescription = "Back" },
                    contentAlignment = Alignment.Center,
                ) { LineArrow(Modifier.size(width = 22.dp, height = 12.dp), color = Ink, pointsLeft = true) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Gutter)) {
                Spacer(Modifier.height(18.dp))
                Kicker("Settings")
                Spacer(Modifier.height(14.dp))
                SplitHeadline("Account &", "privacy.", maxSize = 40f)
                Spacer(Modifier.height(30.dp))

                Section("Account")
                Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("EMAIL", style = MaterialTheme.typography.labelSmall, color = MutedInk, modifier = Modifier.width(90.dp))
                    Text(state.email.ifBlank { "Signed in with Google" }, style = MaterialTheme.typography.bodyLarge, color = Ink)
                }
                Hairline()

                Spacer(Modifier.height(30.dp))
                Section("Privacy")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = !state.discoverable, role = Role.Switch) { viewModel.setDiscoverable(!it) }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Pause my profile", style = MaterialTheme.typography.bodyLarge, color = Ink)
                        Spacer(Modifier.height(3.dp))
                        Text(
                            "You won't appear in Discover. Your matches can still message you.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedInk,
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    SquareSwitch(checked = !state.discoverable)
                }
                Hairline()

                Spacer(Modifier.height(30.dp))
                Section("Blocked")
                if (state.blocked.isEmpty()) {
                    Text("You haven't blocked anyone.", style = MaterialTheme.typography.bodyMedium, color = MutedInk, modifier = Modifier.padding(vertical = 14.dp))
                } else {
                    state.blocked.forEach { b ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(b.profile?.name ?: "Former member", style = MaterialTheme.typography.bodyLarge, color = Ink, modifier = Modifier.weight(1f))
                            CapsButton("Unblock", { viewModel.unblock(b.uid) })
                        }
                        Hairline()
                    }
                }

                if (state.error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(state.error.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = Terracotta)
                }

                Spacer(Modifier.height(36.dp))
                OutlineAction("Sign out", viewModel::signOut)
                Spacer(Modifier.height(22.dp))

                // Account deletion — required, and made deliberate rather than hidden.
                Text(
                    "DELETE ACCOUNT",
                    style = MaterialTheme.typography.labelSmall,
                    color = Terracotta,
                    modifier = Modifier.clickable(role = Role.Button, onClick = viewModel::askDelete).padding(vertical = 12.dp),
                )
                AnimatedVisibility(state.needsFreshLogin) {
                    Column(Modifier.fillMaxWidth().background(Oatmeal.copy(alpha = 0.55f)).padding(16.dp)) {
                        Text(
                            "For your security, sign out and back in, then delete your account within a few minutes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlineAction("Sign out now", viewModel::signOut)
                    }
                }
                AnimatedVisibility(state.confirmingDelete) {
                    Column(Modifier.fillMaxWidth().border(1.dp, Terracotta, MaterialTheme.shapes.extraSmall).padding(16.dp)) {
                        Text(
                            "This permanently deletes your profile, photos and lifestyle answers, and removes you from your matches. It can't be undone.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Ink,
                        )
                        Spacer(Modifier.height(14.dp))
                        PrimaryAction("Delete permanently", viewModel::deleteAccount, loading = state.deleting, loadingText = "Deleting")
                        Spacer(Modifier.height(8.dp))
                        OutlineAction("Keep my account", viewModel::cancelDelete, enabled = !state.deleting)
                    }
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = MutedInk)
    Spacer(Modifier.height(8.dp))
    Hairline()
}

/** A rectangular switch: a hairline track with a square knob that slides and fills. */
@Composable
private fun SquareSwitch(checked: Boolean) {
    val knob by animateDpAsState(if (checked) 20.dp else 0.dp, label = "knob")
    val track by animateColorAsState(if (checked) Forest else Straw, label = "track")
    Box(Modifier.width(42.dp).height(22.dp).border(1.dp, track, MaterialTheme.shapes.extraSmall).padding(3.dp)) {
        Box(Modifier.offset(x = knob).size(16.dp).background(track, MaterialTheme.shapes.extraSmall))
    }
}
