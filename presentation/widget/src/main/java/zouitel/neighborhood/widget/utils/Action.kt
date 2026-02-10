package zouitel.neighborhood.widget.utils

import zouitel.neighborhood.widget.ui.JottingWidget

sealed class Action  {
    class StartActivity: Action() {
        init {
            JottingWidget()
        }
    }
}