package zouitel.neighborhood.markdown.core

import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Immutable data class representing a single block in the editor.
 *
 * @property id Unique identifier for this block
 * @property type The type of block (paragraph, heading, etc.)
 * @property content The content stored in this block
 * @property attributes Block-level document metadata.
 */
@Keep
@Serializable
 data class Block(
    val id: BlockId,
    val type: BlockType,
    val content: BlockContent,
    val attributes: BlockAttributes = BlockAttributes.Default,
) {
    /**
     * Creates a copy with a new type.
     */
     fun withType(newType: BlockType): Block = copy(type = newType)

    /**
     * Creates a copy with new content.
     */
     fun withContent(newContent: BlockContent): Block = copy(content = newContent)

    /**
     * Creates a copy with new block attributes.
     */
     fun withAttributes(newAttributes: BlockAttributes): Block = copy(attributes = newAttributes)

     companion object {
        /**
         * Creates a new paragraph block with the given text and optional spans.
         *
         * @param text Initial plain text content. Defaults to empty.
         * @param spans Rich text style spans applied to ranges within [text]. Defaults to empty.
         */
         fun paragraph(
            text: String = "",
            spans: List<TextSpan> = emptyList(),
        ): Block = Block(
            id = BlockId.Companion.generate(),
            type = BlockType.Paragraph,
            content = BlockContent.Text(text = text, spans = spans),
        )

        /**
         * Creates a new heading block with the given level and text.
         *
         * @param level Heading level from 1 (largest) to 6 (smallest).
         * @param text Initial plain text content. Defaults to empty.
         */
         fun heading(level: Int, text: String = ""): Block = Block(
            id = BlockId.Companion.generate(),
            type = BlockType.Heading(level),
            content = BlockContent.Text(text),
        )

        /**
         * Creates a new todo block.
         */
         fun todo(text: String = "", checked: Boolean = false): Block = Block(
            id = BlockId.Companion.generate(),
            type = BlockType.Todo(checked),
            content = BlockContent.Text(text),
        )

        /**
         * Creates a new bullet list item block.
         */
         fun bulletList(text: String = ""): Block = Block(
            id = BlockId.Companion.generate(),
            type = BlockType.BulletList,
            content = BlockContent.Text(text),
        )

        /**
         * Creates a new numbered list item block.
         */
         fun numberedList(text: String = "", number: Int = 1): Block = Block(
            id = BlockId.Companion.generate(),
            type = BlockType.NumberedList(number),
            content = BlockContent.Text(text),
        )

        /**
         * Creates a new divider block.
         */
         fun divider(): Block = Block(
            id = BlockId.Companion.generate(),
            type = BlockType.Divider,
            content = BlockContent.Empty,
        )
    }
}
