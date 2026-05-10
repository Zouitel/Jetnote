// NonFocusableTapModifier.kt  — replace the expect file entirely
package zouitel.neighborhood.markdown.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
internal fun Modifier.nonFocusableTap(
    enabled: Boolean,
    onClick: () -> Unit,
): Modifier = this.clickable(
    enabled = enabled,
    indication = null,
    interactionSource = remember { MutableInteractionSource() },
    onClick = onClick,
)