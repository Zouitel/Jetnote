package zouitel.neighborhood.widget.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import cafe.adriel.voyager.core.screen.Screen
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.workspace.WorkplaceActivity

class QuickWidget: GlanceAppWidget(), Screen {

    override suspend fun provideGlance(context: Context, id: GlanceId) = provideContent {
        this.Content()
    }

    @SuppressLint("ResourceType")
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val intent_workplace = Intent(context, WorkplaceActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("start_workplace", true)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
            modifier = GlanceModifier
                .height(70.dp)
                .clickable(actionStartActivity(intent_workplace))
                .background(ColorProvider(CommonColors.prime))
                .padding(end = 20.dp)
        ){
            SquareIconButton(
                ImageProvider(CommonIcons.PLUS),
                null,
                {},
                GlanceModifier,
                backgroundColor = ColorProvider(CommonColors.prime),
                contentColor = ColorProvider(Color.White)
            )

            Text("Compose", style = TextStyle(fontSize = 20.sp, color = ColorProvider(Color.White)) )
        }
    }
}