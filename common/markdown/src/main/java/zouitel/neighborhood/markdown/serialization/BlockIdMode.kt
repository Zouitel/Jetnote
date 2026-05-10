package zouitel.neighborhood.markdown.serialization

/**
 * Controls how block IDs are handled during document decode.
 */
enum class BlockIdMode {
    /** Use the ID from the serialized JSON. Generate if missing/empty. */
    Preserve,

    /** Always generate a fresh [zouitel.neighborhood.markdown.core.BlockId] regardless of the value in JSON. */
    Regenerate,
}
