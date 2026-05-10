package zouitel.neighborhood.markdown.serialization

/**
 * Controls how unsupported value types in custom data maps are handled during encode.
 */
enum class CustomDataMode {
    /** Throw [IllegalArgumentException] on unsupported value types. */
    Strict,

    /** Silently skip keys with unsupported value types. */
    LenientSkipUnsupported,
}
