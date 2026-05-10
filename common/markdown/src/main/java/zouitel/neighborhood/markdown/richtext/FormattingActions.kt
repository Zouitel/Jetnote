package zouitel.neighborhood.markdown.richtext

import androidx.compose.runtime.Stable
import zouitel.neighborhood.markdown.core.SpanStyle

/**
 * Action interface for formatting operations. Implementations resolve
 * the focused block and selection at invocation time and delegate to
 * [SpanActionDispatcher]. No-op when formatting is disallowed.
 */
@Stable
 interface FormattingActions {
     fun toggleStyle(style: SpanStyle)
     fun applyStyle(style: SpanStyle)
     fun removeStyle(style: SpanStyle)
}
