package zouitel.neighborhood.markdown.indentation

import androidx.compose.runtime.Stable

/**
 *  action interface for structural block indentation commands.
 *
 * Consumers can read [zouitel.neighborhood.markdown.ui.LocalIndentationActions]
 * from custom editor chrome and call these methods without dispatching reducer
 * actions directly.
 */
@Stable
 interface IndentationActions {
    /**
     * Indents the current supported selection or focused block by one level.
     */
     fun indentForward()

    /**
     * Outdents the current supported selection or focused block by one level.
     */
     fun indentBackward()
}
