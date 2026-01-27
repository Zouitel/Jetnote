package zouitel.neighborhood.widget.receivers

import androidx.glance.appwidget.GlanceAppWidgetReceiver
import zouitel.neighborhood.widget.ui.ListWidget

class ListReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ListWidget()
}