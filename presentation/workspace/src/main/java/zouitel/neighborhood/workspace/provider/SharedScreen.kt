package zouitel.neighborhood.workspace.provider

import cafe.adriel.voyager.core.registry.ScreenProvider
import zouitel.neighborhood.workspace.model.Data

sealed class SharedScreen: ScreenProvider {
    data object MainScreen: SharedScreen()
    data class Workplace(
        val data: Data,
        val isNew: Boolean
    ): SharedScreen()
    data class Tags(
        val uid: String,
        val background: Int?
    ): SharedScreen()

    data class FeedbackSheetScreen(val background: Int?): SharedScreen()
    data object FeedbackScreen: SharedScreen()
}