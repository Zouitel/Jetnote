package zouitel.neighborhood.markdown.core

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

private const val MIN_INDENTATION_LEVEL_VALUE: Int = 0
private const val MAX_INDENTATION_LEVEL_VALUE: Int = 5
private const val DEFAULT_INDENTATION_LEVEL_VALUE: Int = MIN_INDENTATION_LEVEL_VALUE

/**
 * Block-level metadata that belongs to the document model rather than a block type.
 *
 * @property indentationLevel Persistent outline depth for blocks that participate in indentation.
 */
@Keep
@Serializable
 data class BlockAttributes(
    val indentationLevel: Int = DEFAULT_INDENTATION_LEVEL_VALUE,
) {
    init {
        require(indentationLevel in MIN_INDENTATION_LEVEL_VALUE..MAX_INDENTATION_LEVEL_VALUE) {
            "Block indentation level must be between $MIN_INDENTATION_LEVEL_VALUE and " +
                "$MAX_INDENTATION_LEVEL_VALUE, got $indentationLevel"
        }
    }

     companion object {
        internal const val MIN_INDENTATION_LEVEL: Int = MIN_INDENTATION_LEVEL_VALUE
        internal const val MAX_INDENTATION_LEVEL: Int = MAX_INDENTATION_LEVEL_VALUE
        internal const val DEFAULT_INDENTATION_LEVEL: Int = DEFAULT_INDENTATION_LEVEL_VALUE

         val Default: BlockAttributes = BlockAttributes()
    }
}
