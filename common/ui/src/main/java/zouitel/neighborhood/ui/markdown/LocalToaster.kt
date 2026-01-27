package zouitel.neighborhood.ui.markdown

import androidx.compose.runtime.staticCompositionLocalOf
import com.dokar.sonner.ToasterState

val LocalToaster = staticCompositionLocalOf<ToasterState> { error("Not provided") }
