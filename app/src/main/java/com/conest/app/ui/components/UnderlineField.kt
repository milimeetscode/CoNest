package com.conest.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.conest.app.ui.theme.Forest
import com.conest.app.ui.theme.Ink
import com.conest.app.ui.theme.MutedInk
import com.conest.app.ui.theme.Straw
import com.conest.app.ui.theme.Terracotta

/**
 * Editorial input: small-caps label above, text set on a single rule — like a form printed on
 * paper rather than a box on a screen. The rule darkens to forest on focus, terracotta on error.
 */
@Composable
fun UnderlineField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    error: String? = null,
    supporting: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val accent by animateColorAsState(
        when {
            error != null -> Terracotta
            focused -> Forest
            else -> Straw
        },
        label = "rule",
    )
    val labelColor by animateColorAsState(
        when {
            error != null -> Terracotta
            focused -> Forest
            else -> MutedInk
        },
        label = "label",
    )
    val ruleWeight by animateDpAsState(if (focused || error != null) 1.5.dp else 1.dp, label = "weight")

    Column(modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = labelColor)
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                minLines = minLines,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink, fontSize = 17.sp),
                cursorBrush = SolidColor(Forest),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                interactionSource = interaction,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp),
                decorationBox = { inner ->
                    Box(contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                color = MutedInk.copy(alpha = 0.55f),
                            )
                        }
                        inner()
                    }
                },
            )
            trailing?.invoke()
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(ruleWeight)
                .background(accent),
        )
        AnimatedVisibility(
            visible = error != null || supporting != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(6.dp))
                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = Terracotta)
                } else {
                    supporting?.invoke()
                }
            }
        }
    }
}
