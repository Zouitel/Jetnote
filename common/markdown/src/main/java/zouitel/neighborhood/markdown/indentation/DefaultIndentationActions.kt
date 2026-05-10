package zouitel.neighborhood.markdown.indentation

import androidx.compose.runtime.Stable
import zouitel.neighborhood.markdown.action.EditorAction
import zouitel.neighborhood.markdown.action.IndentBackward
import zouitel.neighborhood.markdown.action.IndentForward

/**
 * Default [IndentationActions] implementation used by [zouitel.neighborhood.markdown.ui.CascadeEditor].
 *
 * The state provider is evaluated at invocation time so toolbar buttons and
 * keyboard/custom chrome cannot dispatch stale indentation commands after focus,
 * selection, or document structure changes.
 */
@Stable
class DefaultIndentationActions(
    private val stateProvider: () -> IndentationState,
    private val dispatchAction: (EditorAction) -> Unit,
) : IndentationActions {

    override fun indentForward() {
        if (!stateProvider().canIndentForward) return
        dispatchAction(IndentForward)
    }

    override fun indentBackward() {
        if (!stateProvider().canIndentBackward) return
        dispatchAction(IndentBackward)
    }
}
