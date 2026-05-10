package zouitel.neighborhood.markdown.serialization

import zouitel.neighborhood.markdown.core.Block

/**
 * Result of a document decode operation, containing the decoded blocks and any warnings.
 */
data class DocumentDecodeResult(
    val blocks: List<Block>,
    val warnings: List<DocumentDecodeWarning>,
)
