package zouitel.neighborhood.markdown.serialization

/**
 * Options for [DocumentSchema.encode] / [DocumentSchema.encodeToString].
 */
data class DocumentEncodeOptions(
    val customDataMode: CustomDataMode = CustomDataMode.Strict,
)
