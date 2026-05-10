package zouitel.neighborhood.markdown.registry

import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.markdown.core.BlockContent
import zouitel.neighborhood.markdown.core.BlockType
import zouitel.neighborhood.markdown.core.UnknownBlockType
import zouitel.neighborhood.markdown.slash.BuiltInBlockSlashBehavior
import zouitel.neighborhood.markdown.slash.BuiltInSlashCommandSpec

/**
 * Central registry for block types, descriptors, and renderers.
 *
 * The registry serves as a single point of registration for:
 * - Block descriptors (metadata for slash commands)
 * - Block renderers (composable UI for each type)
 *
 * Use [createDefault] to get a registry pre-populated with built-in types.
 */
 class BlockRegistry {
    private val descriptors = mutableMapOf<String, BlockDescriptor>()
    private val renderers = mutableMapOf<String, BlockRenderer<*>>()
    private var unknownBlockFallback: BlockRenderer<*>? = null

    /**
     * Registers a block descriptor.
     */
     fun registerDescriptor(descriptor: BlockDescriptor) {
        descriptors[descriptor.typeId] = descriptor
    }

    /**
     * Registers a block renderer.
     */
     fun <T : BlockType> registerRenderer(typeId: String, renderer: BlockRenderer<T>) {
        renderers[typeId] = renderer
    }

    /**
     * Registers both a descriptor and renderer for a block type.
     */
     fun <T : BlockType> register(descriptor: BlockDescriptor, renderer: BlockRenderer<T>) {
        registerDescriptor(descriptor)
        registerRenderer(descriptor.typeId, renderer)
    }

    /**
     * Gets a descriptor by type ID.
     */
     fun getDescriptor(typeId: String): BlockDescriptor? = descriptors[typeId]

    /**
     * Gets a renderer by type ID.
     */
     fun getRenderer(typeId: String): BlockRenderer<*>? = renderers[typeId]

    /**
     * Gets a renderer for a [BlockType].
     *
     * Looks up the renderer by [BlockType.typeId]. If no renderer is registered
     * and the type is [UnknownBlockType], returns the fallback set via
     * [setUnknownBlockRenderer]. Returns `null` for any other unregistered type
     * (signals a missing registration, not an unknown deserialized type).
     */
     fun getRenderer(blockType: BlockType): BlockRenderer<*>? {
        renderers[blockType.typeId]?.let { return it }
        if (blockType is UnknownBlockType) return unknownBlockFallback
        return null
    }

    /**
     * Sets the fallback renderer used for [UnknownBlockType] blocks.
     */
     fun setUnknownBlockRenderer(renderer: BlockRenderer<*>) {
        unknownBlockFallback = renderer
    }

    /**
     * Gets all registered descriptors.
     */
     fun getAllDescriptors(): List<BlockDescriptor> = descriptors.values.toList()

    /**
     * Searches for descriptors matching a query.
     * Results are sorted by relevance.
     *
     * Note: slash command search is handled by [SlashCommandRegistry][zouitel.neighborhood.markdown.slash.SlashCommandRegistry].
     * This method is for general-purpose descriptor lookup.
     */
     fun search(query: String): List<BlockDescriptor> {
        return descriptors.values
            .filter { it.matches(query) }
            .sortedByDescending { it.relevanceScore(query) }
    }


    /**
     * Creates a block by type ID using the registered factory.
     * Returns null if the type is not registered.
     */
     fun createBlock(typeId: String): Block? {
        return getDescriptor(typeId)?.createBlock()
    }

    /**
     * Checks if a type ID is registered.
     */
     fun isRegistered(typeId: String): Boolean = typeId in descriptors

     companion object {
        /**
         * Creates a registry with all built-in block types pre-registered.
         * Renderers are NOT included - use this with your own renderers.
         */
         fun createDefault(): BlockRegistry = BlockRegistry().apply {
            registerBuiltInDescriptors()
        }

        /**
         * Creates an empty registry.
         */
         fun create(): BlockRegistry = BlockRegistry()
    }
}

/**
 * Registers all built-in block type descriptors.
 */
private fun BlockRegistry.registerBuiltInDescriptors() {
    // Paragraph — text-capable convertible, ConvertInPlace
    registerDescriptor(
        BlockDescriptor(
            typeId = "paragraph",
            displayName = "Paragraph",
            description = "Plain text paragraph",
            keywords = listOf("text", "p"),
            slash = BuiltInSlashCommandSpec(
                behavior = BuiltInBlockSlashBehavior.ConvertInPlace,
            ),
            factory = { id ->
                Block(id, BlockType.Paragraph, BlockContent.Text(""))
            }
        )
    )

    // Headings 1-6 — text-capable convertible, ConvertInPlace
    for (level in 1..6) {
        registerDescriptor(
            BlockDescriptor(
                typeId = "heading_$level",
                displayName = "Heading $level",
                description = "Heading level $level",
                keywords = listOf("h$level", "heading", "title"),
                slash = BuiltInSlashCommandSpec(
                    behavior = BuiltInBlockSlashBehavior.ConvertInPlace,
                ),
                factory = { id ->
                    Block(id, BlockType.Heading(level), BlockContent.Text(""))
                }
            )
        )
    }

    // Todo — text-capable convertible, ConvertInPlace
    registerDescriptor(
        BlockDescriptor(
            typeId = "todo",
            displayName = "To-do",
            description = "Task with checkbox",
            keywords = listOf("checkbox", "task", "check", "todo"),
            slash = BuiltInSlashCommandSpec(
                behavior = BuiltInBlockSlashBehavior.ConvertInPlace,
            ),
            factory = { id ->
                Block(id, BlockType.Todo(checked = false), BlockContent.Text(""))
            }
        )
    )

    // Bullet List — text-capable convertible, ConvertInPlace
    registerDescriptor(
        BlockDescriptor(
            typeId = "bullet_list",
            displayName = "Bullet List",
            description = "Unordered list item",
            keywords = listOf("list", "bullet", "ul", "unordered"),
            slash = BuiltInSlashCommandSpec(
                behavior = BuiltInBlockSlashBehavior.ConvertInPlace,
            ),
            factory = { id ->
                Block(id, BlockType.BulletList, BlockContent.Text(""))
            }
        )
    )

    // Numbered List — text-capable convertible, ConvertInPlace
    registerDescriptor(
        BlockDescriptor(
            typeId = "numbered_list",
            displayName = "Numbered List",
            description = "Ordered list item",
            keywords = listOf("list", "number", "ol", "ordered"),
            slash = BuiltInSlashCommandSpec(
                behavior = BuiltInBlockSlashBehavior.ConvertInPlace,
            ),
            factory = { id ->
                Block(id, BlockType.NumberedList(number = 1), BlockContent.Text(""))
            }
        )
    )

    // Quote — text-capable convertible, ConvertInPlace
    registerDescriptor(
        BlockDescriptor(
            typeId = "quote",
            displayName = "Quote",
            description = "Quoted text block",
            keywords = listOf("blockquote", "citation"),
            slash = BuiltInSlashCommandSpec(
                behavior = BuiltInBlockSlashBehavior.ConvertInPlace,
            ),
            factory = { id ->
                Block(id, BlockType.Quote, BlockContent.Text(""))
            }
        )
    )

    // Divider — non-text, AlwaysInsert
    registerDescriptor(
        BlockDescriptor(
            typeId = "divider",
            displayName = "Divider",
            description = "Horizontal line separator",
            keywords = listOf("hr", "line", "separator", "horizontal"),
            slash = BuiltInSlashCommandSpec(
                behavior = BuiltInBlockSlashBehavior.AlwaysInsert,
            ),
            factory = { id ->
                Block(id, BlockType.Divider, BlockContent.Empty)
            }
        )
    )
}
