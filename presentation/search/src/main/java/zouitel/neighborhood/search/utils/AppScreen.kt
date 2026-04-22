package zouitel.neighborhood.search.utils

import zouitel.neighborhood.logic.R

enum class AppScreen(val screen: Int) {
    MAIN_SCREEN(R.string.screen_notes),
    CHAT_SCREEN(R.string.ai_chat),
    TAGS_SCREEN(R.string.screen_tags),
    SETTINGS_SCREEN(R.string.screen_settings),
//    WIDGETS_SCREEN("Widgets"),
    ARCHIVE_SCREEN(R.string.screen_archive),
    FEEDBACK_AND_HELP(R.string.screen_feedback),
    ABOUT_SCREEN(R.string.screen_about)
}