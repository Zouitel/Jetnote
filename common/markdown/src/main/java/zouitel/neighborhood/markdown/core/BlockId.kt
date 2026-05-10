package zouitel.neighborhood.markdown.core

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Type-safe wrapper around a block's unique string identifier.
 *
 * @property value The underlying string identifier.
 */
@JvmInline
@Serializable
 value class BlockId( val value: String) {
     companion object {
        /** Generates a new unique [BlockId] using a random UUID. */
        @OptIn(ExperimentalUuidApi::class)
         fun generate(): BlockId = BlockId(Uuid.random().toString())
    }
}
