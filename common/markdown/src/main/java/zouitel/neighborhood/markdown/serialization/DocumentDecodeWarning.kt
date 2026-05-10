package zouitel.neighborhood.markdown.serialization

/**
 * Warnings emitted during document decode.
 *
 * `blockIndex` values refer to the original JSON `blocks` array index, not the
 * decoded block-list position after malformed entries have been skipped.
 *
 * This is a sealed class — consumers should include an `else` branch in `when` expressions
 * to handle future warning subclasses gracefully.
 */
sealed class DocumentDecodeWarning {

    /** Unknown block type preserved as [zouitel.neighborhood.markdown.core.UnknownBlockType]. */
    data class UnknownBlockTypePreserved(
        val blockIndex: Int,
        val typeId: String,
    ) : DocumentDecodeWarning()

    /** Unknown content kind decoded as [zouitel.neighborhood.markdown.core.BlockContent.Custom]. */
    data class UnknownContentKind(
        val blockIndex: Int,
        val kind: String,
    ) : DocumentDecodeWarning()

    /** Duplicate block ID was replaced with a freshly generated one. */
    data class DuplicateIdRegenerated(
        val blockIndex: Int,
        val originalId: String,
        val newId: String,
    ) : DocumentDecodeWarning()

    /** Empty or missing block ID was generated. */
    data class MissingIdRegenerated(
        val blockIndex: Int,
    ) : DocumentDecodeWarning()

    /** Block had invalid required fields and was skipped. */
    data class MalformedBlockSkipped(
        val blockIndex: Int,
        val reason: String,
    ) : DocumentDecodeWarning()

    /** Lenient mode dropped an unsupported custom data value. */
    data class UnsupportedCustomDataDropped(
        val blockIndex: Int,
        val key: String,
        val valueType: String,
    ) : DocumentDecodeWarning()

    /** A block type parameter was invalid and a fallback was used. */
    data class InvalidBlockTypeParam(
        val blockIndex: Int,
        val typeId: String,
        val param: String,
        val fallback: String,
    ) : DocumentDecodeWarning()

    /** A block attribute parameter was invalid and a fallback was used. */
    data class InvalidBlockAttributeParam(
        val blockIndex: Int,
        val param: String,
        val fallback: String,
    ) : DocumentDecodeWarning()
}
