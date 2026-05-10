package zouitel.neighborhood.markdown.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Sealed hierarchy representing the content of a block.
 */
@Serializable
 sealed interface BlockContent {
    /**
     * Text content for blocks like paragraphs, headings, lists.
     *
     * @property text The plain text content
     * @property spans Rich text style spans applied to ranges within [text].
     *   Uses visible-coordinate half-open ranges. Default is empty (plain text).
     */
    @Serializable
     data class Text(
        val text: String,
        val spans: List<TextSpan> = emptyList(),
    ) : BlockContent

    /**
     * Empty content for blocks like dividers.
     */
    @Serializable
     data object Empty : BlockContent

    /**
     * Custom content for extension blocks.
     * @param typeId Identifier matching the custom block type
     * @param data Arbitrary data map for the custom content
     */
    @Serializable
     data class Custom(
        val typeId: String,
        val data: Map<String, JsonElement> = emptyMap()
    ) : BlockContent
}
