package zouitel.neighborhood.navigator.templates

import zouitel.neighborhood.markdown.core.Block
import zouitel.neighborhood.markdown.core.BlockId
import zouitel.neighborhood.workspace.model.Data

/**
 * Bundled templates are intentionally local and available without an account or network.
 * Each template creates fresh block IDs when applied to a note.
 */
data class NoteTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val preview: String,
    val blocks: () -> List<Block>,
    val isFavorite: Boolean = false,
    val isCustom: Boolean = false
) {
    fun toNoteData() = Data(
        title = title,
        blocks = blocks().map { it.copy(id = BlockId.generate()) },
        position = 2
    )
}

object NoteTemplateCatalog {
    val categories = listOf("All", "Personal", "Work", "Study", "Planning")

    val templates = listOf(
        NoteTemplate(
            id = "daily-journal",
            title = "Daily Journal",
            description = "Capture the day, your thoughts, and small wins.",
            category = "Personal",
            preview = "Today's highlights · Gratitude · Reflection",
            blocks = {
                listOf(
                    Block.heading(2, "Today's highlights"),
                    Block.paragraph("What happened today?"),
                    Block.heading(2, "Grateful for"),
                    Block.bulletList(""),
                    Block.heading(2, "A thought to remember"),
                    Block.paragraph("")
                )
            }
        ),
        NoteTemplate(
            id = "meeting-notes",
            title = "Meeting Notes",
            description = "Keep discussions, decisions, and follow-ups together.",
            category = "Work",
            preview = "Attendees · Agenda · Decisions · Action items",
            blocks = {
                listOf(
                    Block.paragraph("Date: "),
                    Block.paragraph("Attendees: "),
                    Block.heading(2, "Agenda"),
                    Block.bulletList(""),
                    Block.heading(2, "Discussion notes"),
                    Block.paragraph(""),
                    Block.heading(2, "Decisions"),
                    Block.bulletList(""),
                    Block.heading(2, "Action items"),
                    Block.todo("")
                )
            }
        ),
        NoteTemplate(
            id = "project-plan",
            title = "Project Plan",
            description = "Define the outcome, milestones, risks, and next steps.",
            category = "Work",
            preview = "Objective · Milestones · Risks · Next steps",
            blocks = {
                listOf(
                    Block.heading(2, "Objective"),
                    Block.paragraph("What does success look like?"),
                    Block.heading(2, "Milestones"),
                    Block.todo(""),
                    Block.heading(2, "Risks and dependencies"),
                    Block.bulletList(""),
                    Block.heading(2, "Next steps"),
                    Block.todo("")
                )
            }
        ),
        NoteTemplate(
            id = "study-notes",
            title = "Study Notes",
            description = "Organize a topic into concepts, examples, and questions.",
            category = "Study",
            preview = "Key concepts · Examples · Questions · Summary",
            blocks = {
                listOf(
                    Block.heading(2, "Key concepts"),
                    Block.bulletList(""),
                    Block.heading(2, "Examples"),
                    Block.paragraph(""),
                    Block.heading(2, "Questions"),
                    Block.todo(""),
                    Block.heading(2, "Summary"),
                    Block.paragraph("")
                )
            }
        ),
        NoteTemplate(
            id = "reading-notes",
            title = "Reading Notes",
            description = "Save useful ideas and quotes from books or articles.",
            category = "Study",
            preview = "Source · Main ideas · Quotes · Takeaways",
            blocks = {
                listOf(
                    Block.paragraph("Source: "),
                    Block.heading(2, "Main ideas"),
                    Block.bulletList(""),
                    Block.heading(2, "Quotes"),
                    Block.paragraph(""),
                    Block.heading(2, "My takeaways"),
                    Block.paragraph("")
                )
            }
        ),
        NoteTemplate(
            id = "weekly-planner",
            title = "Weekly Planner",
            description = "Plan priorities and track the week day by day.",
            category = "Planning",
            preview = "Top priorities · Monday–Sunday · Review",
            blocks = {
                listOf(
                    Block.heading(2, "Top priorities"),
                    Block.todo(""),
                    Block.todo(""),
                    Block.todo(""),
                    Block.divider(),
                    Block.heading(2, "Monday"),
                    Block.todo(""),
                    Block.heading(2, "Tuesday"),
                    Block.todo(""),
                    Block.heading(2, "Wednesday"),
                    Block.todo(""),
                    Block.heading(2, "Thursday"),
                    Block.todo(""),
                    Block.heading(2, "Friday"),
                    Block.todo(""),
                    Block.heading(2, "Weekend"),
                    Block.todo(""),
                    Block.heading(2, "Weekly review"),
                    Block.paragraph("")
                )
            }
        ),
        NoteTemplate(
            id = "task-list",
            title = "Task List",
            description = "Turn a busy day into a clear, actionable checklist.",
            category = "Planning",
            preview = "Must do · Should do · If there's time",
            blocks = {
                listOf(
                    Block.heading(2, "Must do"),
                    Block.todo(""),
                    Block.heading(2, "Should do"),
                    Block.todo(""),
                    Block.heading(2, "If there's time"),
                    Block.todo("")
                )
            }
        ),
        NoteTemplate(
            id = "travel-plan",
            title = "Travel Planner",
            description = "Collect trip details, places to visit, and packing tasks.",
            category = "Personal",
            preview = "Destination · Itinerary · Places · Packing",
            blocks = {
                listOf(
                    Block.heading(2, "Trip details"),
                    Block.paragraph("Destination: "),
                    Block.paragraph("Dates: "),
                    Block.heading(2, "Itinerary"),
                    Block.paragraph(""),
                    Block.heading(2, "Places to visit"),
                    Block.bulletList(""),
                    Block.heading(2, "Packing checklist"),
                    Block.todo("")
                )
            }
        )
    )
}
