package zouitel.neighborhood.markdown.custom

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toColorLong
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import zouitel.neighborhood.markdown.R
import zouitel.neighborhood.markdown.core.SpanStyle
import zouitel.neighborhood.markdown.indentation.IndentationActions
import zouitel.neighborhood.markdown.indentation.IndentationState
import zouitel.neighborhood.markdown.richtext.FormattingActions
import zouitel.neighborhood.markdown.richtext.FormattingState
import zouitel.neighborhood.markdown.richtext.StyleStatus
import zouitel.neighborhood.markdown.theme.CascadeEditorColors
import zouitel.neighborhood.markdown.theme.CascadeEditorStrings
import zouitel.neighborhood.markdown.theme.CascadeEditorTypography
import zouitel.neighborhood.markdown.theme.LocalCascadeStrings
import zouitel.neighborhood.markdown.theme.LocalCascadeTheme
import zouitel.neighborhood.markdown.ui.RichTextToolbarConfig
import zouitel.neighborhood.markdown.ui.SlashActionButton
import zouitel.neighborhood.markdown.ui.ToolbarButtonSpec
import zouitel.neighborhood.markdown.ui.ToolbarIconButton
import zouitel.neighborhood.markdown.ui.buttonDisplayText
import zouitel.neighborhood.markdown.ui.buttonTextStyle
import zouitel.neighborhood.markdown.ui.localizedLabel
import zouitel.neighborhood.markdown.ui.nonFocusableTap

@Composable
    fun WritersPaletteToolbar(
        formattingState: State<FormattingState>,
        actions: FormattingActions,
//        onSlashInsert: () -> Unit,
        onInsertDate: () -> Unit,
    ) {
        val state = formattingState.value
        val canFormat = state.canFormat

        val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val colors = LocalCascadeTheme.current.colors
    val typography = LocalCascadeTheme.current.typography
    val strings = LocalCascadeStrings.current

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusProperties { canFocus = false }
                .background(
                    surfaceColor,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                )
                .graphicsLayer { alpha = if (canFormat) 1f else 0.4f }
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Group 1: Text Style
            ButtonGroup {
//                SlashActionButton(
//                    enabled = state.canFormat,
//                    colors = colors,
//                    typography = typography,
//                    strings = strings,
//                    onClick = onSlashInsert,
//                )

//                if (config.showIndentation) {
//                    ToolbarIconButton(
//                        icon = R.drawable.ic_format_indent_decrease,
//                        label = strings.indentBackward,
//                        enabled = indentationState.value.canIndentBackward,
//                        colors = colors,
//                        onClick = indentationActions::indentBackward,
//                    )
//                    ToolbarIconButton(
//                        icon = R.drawable.ic_format_indent_increase,
//                        label = strings.indentForward,
//                        enabled = indentationState.value.canIndentForward,
//                        colors = colors,
//                        onClick = indentationActions::indentForward,
//                    )
//                }

                StyleToggleButton(
                    label = "B",
                    textStyle = TextStyle(fontWeight = FontWeight.Bold),
                    status = state.styleStatusOf(SpanStyle.Bold),
                    enabled = canFormat,
                    onClick = { actions.toggleStyle(SpanStyle.Bold) },
                )
                StyleToggleButton(
                    label = "I",
                    textStyle = TextStyle(fontStyle = FontStyle.Italic),
                    status = state.styleStatusOf(SpanStyle.Italic),
                    enabled = canFormat,
                    onClick = { actions.toggleStyle(SpanStyle.Italic) },
                )
                StyleToggleButton(
                    label = "U",
                    textStyle = TextStyle(textDecoration = TextDecoration.Underline),
                    status = state.styleStatusOf(SpanStyle.Underline),
                    enabled = canFormat,
                    onClick = { actions.toggleStyle(SpanStyle.Underline) },
                )
            }

            // Group 2: Decoration
            ButtonGroup {
                StyleToggleButton(
                    label = "S",
                    textStyle = TextStyle(textDecoration = TextDecoration.LineThrough),
                    status = state.styleStatusOf(SpanStyle.StrikeThrough),
                    enabled = canFormat,
                    onClick = { actions.toggleStyle(SpanStyle.StrikeThrough) },
                )
                StyleToggleButton(
                    label = "<>",
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                    status = state.styleStatusOf(SpanStyle.InlineCode),
                    enabled = canFormat,
                    onClick = { actions.toggleStyle(SpanStyle.InlineCode) },
                )
                StyleToggleButton(
                    label = "H",
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace),
                    status = state.styleStatusOf(SpanStyle.Highlight(Color.Yellow.toColorLong())),
                    enabled = canFormat,
                    onClick = { actions.toggleStyle(SpanStyle.Highlight(Color.Yellow.toColorLong())) },
                )
            }

            // Group 3: Insert
            ButtonGroup {
                ActionButton(
                    label = "Date",
                    enabled = canFormat,
                    onClick = onInsertDate,
                )
            }

            // Clear button
            ClearFormattingButton(
                state = state,
                actions = actions,
            )
        }
    }

// Button Group (pill container)

    @Composable
    private fun ButtonGroup(
        content: @Composable () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    RoundedCornerShape(10.dp),
                )
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            content()
        }
    }

// Style Toggle Button
@Composable
private fun ToolbarToggleButton(
    spec: ToolbarButtonSpec,
    status: StyleStatus,
    enabled: Boolean,
    colors: CascadeEditorColors,
    typography: CascadeEditorTypography,
    strings: CascadeEditorStrings,
    onClick: () -> Unit,
) {
    val active = status == StyleStatus.FullyActive
    val partial = status == StyleStatus.Partial

    val backgroundColor = when {
        !enabled -> Color.Transparent
        active -> colors.primary
        partial -> colors.primary.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val contentColor = when {
        !enabled -> colors.toolbarIconDisabled
        active -> colors.onPrimary
        else -> colors.toolbarIcon
    }

    val shape = RoundedCornerShape(6.dp)

    Box(
        modifier = Modifier
            .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
            .clip(shape)
            .background(backgroundColor)
            .nonFocusableTap(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = localizedLabel(spec, strings) },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(
            text = buttonDisplayText(spec.style),
            style = buttonTextStyle(spec.style, contentColor, typography),
        )
    }
}

    @Composable
    private fun StyleToggleButton(
        label: String,
        textStyle: TextStyle,
        status: StyleStatus,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val onPrimary = MaterialTheme.colorScheme.onPrimary
        val onSurface = MaterialTheme.colorScheme.onSurface

        val targetBackground = when {
            !enabled -> Color.Transparent
            status == StyleStatus.FullyActive -> primary
            status == StyleStatus.Partial -> primary.copy(alpha = 0.15f)
            else -> Color.Transparent
        }

        val targetContentColor = when {
            !enabled -> onSurface.copy(alpha = 0.25f)
            status == StyleStatus.FullyActive -> onPrimary
            status == StyleStatus.Partial -> primary
            else -> onSurface.copy(alpha = 0.4f)
        }

        val targetBorderColor = when {
            !enabled -> Color.Transparent
            status == StyleStatus.FullyActive -> Color.Transparent
            status == StyleStatus.Partial -> primary.copy(alpha = 0.3f)
            else -> onSurface.copy(alpha = 0.15f)
        }

        val backgroundColor = animateColorAsState(targetBackground, tween(200))
        val contentColor = animateColorAsState(targetContentColor, tween(200))
        val borderColor = animateColorAsState(targetBorderColor, tween(200))

        val shape = RoundedCornerShape(8.dp)

        Box(
            modifier = Modifier
                .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                .clip(shape)
                .background(backgroundColor.value, shape)
                .then(
                    if (borderColor.value != Color.Transparent) {
                        Modifier.border(1.dp, borderColor.value, shape)
                    } else {
                        Modifier
                    }
                )
                .nonFocusableTap(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = textStyle.fontWeight ?: FontWeight.Medium,
                    fontStyle = textStyle.fontStyle,
                    textDecoration = textStyle.textDecoration,
                    fontFamily = textStyle.fontFamily,
                    color = contentColor.value,
                ),
            )
        }
    }

// Action Button (non-toggle)

    @Composable
    private fun ActionButton(
        label: String,
        enabled: Boolean,
        onClick: () -> Unit,
    ) {
        val primary = MaterialTheme.colorScheme.primary
        val onSurface = MaterialTheme.colorScheme.onSurface

        val contentColor = animateColorAsState(
            if (enabled) primary else onSurface.copy(alpha = 0.25f),
            tween(200),
        )

        val borderColor = animateColorAsState(
            if (enabled) primary.copy(alpha = 0.3f) else onSurface.copy(alpha = 0.15f),
            tween(200),
        )

        val shape = RoundedCornerShape(8.dp)

        Box(
            modifier = Modifier
                .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                .clip(shape)
                .border(1.dp, borderColor.value, shape)
                .nonFocusableTap(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = contentColor.value,
                ),
            )
        }
    }

// Clear Formatting Button

    @Composable
    private fun ClearFormattingButton(
        state: FormattingState,
        actions: FormattingActions,
    ) {
        val hasAnyFormatting = state.styles.any { (_, status) ->
            status != StyleStatus.Absent
        }
        val enabled = state.canFormat && hasAnyFormatting

        val primary = MaterialTheme.colorScheme.primary
        val onSurface = MaterialTheme.colorScheme.onSurface

        val contentColor = animateColorAsState(
            if (enabled) primary else onSurface.copy(alpha = 0.25f),
            tween(200),
        )

        val borderColor = animateColorAsState(
            if (enabled) primary.copy(alpha = 0.3f) else onSurface.copy(alpha = 0.1f),
            tween(200),
        )

        val shape = RoundedCornerShape(8.dp)

        Box(
            modifier = Modifier
                .sizeIn(minWidth = 40.dp, minHeight = 40.dp)
                .clip(shape)
                .border(1.dp, borderColor.value, shape)
                .nonFocusableTap(enabled = enabled) {
                    state.styles.forEach { (style, status) ->
                        if (status != StyleStatus.Absent) {
                            actions.removeStyle(style)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "CLR",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = contentColor.value,
                ),
            )
        }
    }

// Demo Document

@Composable
    private fun Modifier.nonFocusableTap(
        enabled: Boolean = true,
        onClick: () -> Unit,
    ): Modifier {
        if (!enabled) return this
        return this
            .clickable(onClick = onClick)
            .focusProperties { canFocus = false }
    }
