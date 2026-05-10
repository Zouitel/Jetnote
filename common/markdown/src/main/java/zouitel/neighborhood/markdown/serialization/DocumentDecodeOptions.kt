package zouitel.neighborhood.markdown.serialization

/**
 * Options for [DocumentSchema.decode] / [DocumentSchema.decodeFromString].
 */
data class DocumentDecodeOptions(
    val idMode: BlockIdMode = BlockIdMode.Preserve,
    val duplicateIdMode: DuplicateIdMode = DuplicateIdMode.RegenerateLaterDuplicates,
)
