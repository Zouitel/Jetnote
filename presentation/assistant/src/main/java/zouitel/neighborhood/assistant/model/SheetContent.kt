package zouitel.neighborhood.assistant.model

import zouitel.neighborhood.ai.provider.CustomBody
import zouitel.neighborhood.ai.provider.CustomHeader
import zouitel.neighborhood.ai.provider.Model

sealed class SheetContent {
    data class Quick(val quickMessage: QuickMessage, val onDone: (QuickMessage) -> Unit): SheetContent()
    data class Regex(val messageRegex: AssistantRegex, val onDone: (AssistantRegex) -> Unit): SheetContent()
    data class Header(val header: CustomHeader,val onDone: (CustomHeader) -> Unit): SheetContent()
    data class Body(val body: CustomBody,val onDone: (CustomBody) -> Unit): SheetContent()
    data class Modes(val models: List<Model>, val onDone: (String) -> Unit): SheetContent()
    data class Reset(val confirm: () -> Unit): SheetContent()
    data class Save(val onSave: () -> Unit, val onDiscard: () -> Unit) : SheetContent()
    object None: SheetContent()
}
