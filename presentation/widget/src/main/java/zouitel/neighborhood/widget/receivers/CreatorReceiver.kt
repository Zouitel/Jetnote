package zouitel.neighborhood.widget.receivers

import androidx.glance.appwidget.GlanceAppWidgetReceiver
import zouitel.neighborhood.widget.ui.CreatorWidget

class CreatorReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget = CreatorWidget()
}