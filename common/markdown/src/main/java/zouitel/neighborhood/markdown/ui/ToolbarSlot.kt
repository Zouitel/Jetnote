package zouitel.neighborhood.markdown.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import zouitel.neighborhood.markdown.core.SpanStyle
import zouitel.neighborhood.markdown.richtext.FormattingActions
import zouitel.neighborhood.markdown.richtext.FormattingState

/**
 * Determines what toolbar (if any) is shown at the bottom of the editor.
 */
 sealed interface ToolbarSlot {

    /** Default config-driven toolbar with toggle buttons for built-in styles. */
     data class Default(
        val config: RichTextToolbarConfig = RichTextToolbarConfig.Default,
    ) : ToolbarSlot

    /** No toolbar rendered. */
     data object None : ToolbarSlot

    /**
     * Consumer-provided composable toolbar receiving reactive state and actions.
     *
     * Indentation controls intentionally stay out of this lambda signature for
     * source compatibility. Read [LocalIndentationState] and [LocalIndentationActions]
     * from the composable body when custom toolbar chrome needs indent/outdent.
     *
     * @param trackedStyles Styles to include in [FormattingState.styles].
     *        Defaults to the default toolbar styles. Override to track additional
     *        styles (e.g., [SpanStyle.Custom][io.github.linreal.cascade.editor.core.SpanStyle.Custom]).
     */
     data class Custom(
        val trackedStyles: List<SpanStyle> =
            RichTextToolbarConfig.Default.buttons.map { it.style },
        val content: @Composable (
            formattingState: State<FormattingState>,
            actions: FormattingActions,
        ) -> Unit,
    ) : ToolbarSlot
}
