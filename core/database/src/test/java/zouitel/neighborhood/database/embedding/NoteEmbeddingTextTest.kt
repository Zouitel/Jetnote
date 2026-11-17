package zouitel.neighborhood.database.embedding

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEmbeddingTextTest {
    @Test
    fun extractsOnlyPlainTextFromSerializedBlocks() {
        val blocks = """
            [
              {"id":"1","type":{"kind":"paragraph"},"content":{"kind":"text","text":"A note about hiking."}},
              {"id":"2","type":{"kind":"heading","level":2},"content":{"kind":"text","text":"Mountain routes"}},
              {"id":"3","type":{"kind":"divider"},"content":{"kind":"empty"}}
            ]
        """.trimIndent()

        assertEquals(
            "Travel journal\nA note about hiking.\nMountain routes",
            NoteEmbeddingText.from("Travel journal", blocks),
        )
    }

    @Test
    fun chunksLongTextWithOverlapWithoutDroppingWords() {
        val words = (1..650).map(Int::toString)
        val chunks = NoteEmbeddingText.chunks(words.joinToString(" "), maxWords = 300, overlapWords = 40)

        assertEquals(3, chunks.size)
        assertEquals(300, chunks.first().split(" ").size)
        assertEquals(300, chunks[1].split(" ").size)
        assertEquals(130, chunks.last().split(" ").size)
        assertEquals("300", chunks[0].split(" ").last())
        assertEquals("261", chunks[1].split(" ").first())
    }

    @Test
    fun fallsBackToRawTextForLegacyNonJsonBlocks() {
        assertEquals(
            "Title\nlegacy note body",
            NoteEmbeddingText.from("Title", "legacy note body"),
        )
    }

    @Test
    fun omitsEmptyTextAndMetadata() {
        val blocks = """[{"id":"secret-id","content":{"kind":"text","text":"  "}}]"""

        assertEquals("Title", NoteEmbeddingText.from("Title", blocks))
    }
}
