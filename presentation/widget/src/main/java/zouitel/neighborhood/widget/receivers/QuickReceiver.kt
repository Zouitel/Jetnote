package zouitel.neighborhood.widget.receivers

import androidx.glance.appwidget.GlanceAppWidgetReceiver
import zouitel.neighborhood.widget.ui.QuickWidget

class QuickReceiver: GlanceAppWidgetReceiver() {
    override val glanceAppWidget = QuickWidget()
}