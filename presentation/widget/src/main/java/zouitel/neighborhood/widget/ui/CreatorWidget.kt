package zouitel.neighborhood.widget.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.components.SquareIconButton
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import cafe.adriel.voyager.core.screen.Screen
import zouitel.neighborhood.jotting.QuickActivity
import zouitel.neighborhood.ui.CommonColors
import zouitel.neighborhood.ui.CommonIcons

class  CreatorWidget: GlanceAppWidget(), Screen {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            this@CreatorWidget.Content()
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val intent_record = Intent(context, QuickActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("quick_record", true)
        }
        val intent_photo = Intent(context, QuickActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("quick_photo", true)
        }
        val intent_reminder = Intent(context, QuickActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("quick_reminder", true)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(70.dp)
                .background(ColorProvider(Color.Transparent))
        ) {
            Spacer(GlanceModifier.defaultWeight())

            CircleIconButton(
                ImageProvider(CommonIcons.BELL),
                null,
                actionStartActivity(intent_reminder),
                GlanceModifier.size(70.dp)
            )

            Spacer(GlanceModifier.defaultWeight())

            CircleIconButton(
                ImageProvider(CommonIcons.MIC),
                null,
                actionStartActivity(intent_record),
                GlanceModifier.size(70.dp)
            )

            Spacer(GlanceModifier.defaultWeight())

            CircleIconButton(
                ImageProvider(CommonIcons.CAMERA),
                null,
                actionStartActivity(intent_photo),
                GlanceModifier.size(70.dp)
            )

            Spacer(GlanceModifier.defaultWeight())

            SquareIconButton(
                ImageProvider(CommonIcons.TEXT),
                null,
                actionStartActivity<QuickActivity>(),
                GlanceModifier.size(70.dp),
                backgroundColor = ColorProvider(CommonColors.prime),
                contentColor = ColorProvider(MaterialTheme.colorScheme.surface)
            )

            Spacer(GlanceModifier.defaultWeight())
        }
    }
}
