package zouitel.neighborhood.widget.receivers

import androidx.glance.appwidget.GlanceAppWidgetReceiver
import zouitel.neighborhood.widget.ui.JottingWidget

class QuickReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget = JottingWidget()
}