package zouitel.neighborhood.widget.utils

import zouitel.neighborhood.widget.ui.QuickWidget

sealed class Action  {
    class StartActivity: Action() {
        init {
            QuickWidget()
        }
    }
}