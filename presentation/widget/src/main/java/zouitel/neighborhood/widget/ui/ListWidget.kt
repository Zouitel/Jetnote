package zouitel.neighborhood.widget.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentWidth
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.widget.model.WidgetNote
import zouitel.neighborhood.widget.utils.JsonInstant
import zouitel.neighborhood.workspace.WorkplaceActivity
import zouitel.neighborhood.workspace.model.Priorities

class ListWidget: GlanceAppWidget(), Screen {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            this@ListWidget.Content()
        }
    }

    @SuppressLint("RestrictedApi")
    @Composable
    override fun Content() {
        val widgetModel = getScreenModel<WidgetScreenModel>()
        val notes = remember(widgetModel, widgetModel::observeByDefault).collectAsState()

        Column(
            modifier = GlanceModifier.Companion.background(GlanceTheme.colors.surfaceVariant)
        ) {
            Row(
                verticalAlignment = Alignment.Companion.CenterVertically,
                modifier = GlanceModifier.Companion.fillMaxWidth()
            ) {
                Text(
                    text = "Notes",
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 16.sp
                    ),
                    modifier = GlanceModifier.Companion.padding(10.dp)
                )
            }
            LazyColumn(
                modifier = GlanceModifier.Companion.background(GlanceTheme.colors.surface)
            ) {
                items(notes.value) { note ->
                    NoteItem(note)
                }
            }
        }
    }

    @SuppressLint("RestrictedApi", "ResourceType")
    @Composable
    private fun NoteItem(noteEntity: WidgetNote) {
        val note = noteEntity.data
        val labels = remember { noteEntity.tags }
        val context = LocalContext.current

        val audiosCount =  note.audios.count()
        val remindersCount = note.reminders.count()

        Column(
            horizontalAlignment = Alignment.Companion.Start,
            verticalAlignment = Alignment.Companion.CenterVertically,
            modifier = GlanceModifier.Companion
                .fillMaxWidth()
                .height(90.dp)
                .background(GlanceTheme.colors.surface)
                .padding(5.dp)
                .clickable {
                    val intent = Intent(context, WorkplaceActivity::class.java)
                        .apply {
                            action = Intent.ACTION_VIEW
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            putExtra("start_workplace", true)
                            putExtra("data", JsonInstant.encodeToString(note))
                        }
                    context.startActivity(intent)
                }
        ) {
            Text(
                text = note.title,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 18.sp,
                ),
                maxLines = 1,
                modifier = GlanceModifier.Companion.padding(7.dp)
            )

            Row(
                verticalAlignment = Alignment.Companion.CenterVertically,
                horizontalAlignment = Alignment.Companion.CenterHorizontally,
                modifier = GlanceModifier.Companion.fillMaxWidth().wrapContentWidth()
                    .padding(bottom = 7.dp)
            ) {
                priority(note.priority).let {
                    // U+25CF, •
                    Text(
                        text = "\u25CF " + it.priority,
                        style = TextStyle(
                            color = ColorProvider(it.color),
                            fontSize = 15.sp,
                        ),
                        modifier = GlanceModifier.Companion.padding(6.dp)
                    )
                }

                if (audiosCount > 0) {
                    Text(
                        text = "|",
                        style = TextStyle(
                            color = GlanceTheme.colors.surfaceVariant,
                            fontSize = 24.sp
                        ),
                        modifier = GlanceModifier.Companion.padding(start = 6.dp, end = 6.dp)
                    )

                    Image(
                        provider = ImageProvider(CommonIcons.WAVEFORM_18),
                        contentDescription = null,
                        colorFilter = ColorFilter.Companion.tint(GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.Companion.size(16.dp)
                    )
                    if (audiosCount > 1) {
                        Text(
                            text = " +${audiosCount - 1}",
                            style = TextStyle(
                                color = GlanceTheme.colors.surfaceVariant,
                                fontSize = 12.sp,
                            )
                        )
                    }
                }

                if (remindersCount > 0) {
                    Text(
                        text = "|",
                        style = TextStyle(
                            color = GlanceTheme.colors.surfaceVariant,
                            fontSize = 24.sp
                        ),
                        modifier = GlanceModifier.Companion.padding(start = 6.dp, end = 6.dp)
                    )
                    Image(
                        provider = ImageProvider(CommonIcons.BELL_RING_18),
                        contentDescription = null,
                        colorFilter = ColorFilter.Companion.tint(GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.Companion.size(16.dp)
                    )
                    if (remindersCount > 1) {
                        Text(
                            text = " +${remindersCount - 1}",
                            style = TextStyle(
                                color = GlanceTheme.colors.surfaceVariant,
                                fontSize = 12.sp,
                            )
                        )
                    }
                }

                labels.firstOrNull()?.let {
                    Text(
                        text = "|",
                        style = TextStyle(
                            color = GlanceTheme.colors.surfaceVariant,
                            fontSize = 24.sp
                        ),
                        modifier = GlanceModifier.Companion.padding(start = 6.dp, end = 6.dp)
                    )
                    Image(
                        provider = ImageProvider(CommonIcons.HASTAG),
                        contentDescription = null,
                        colorFilter = ColorFilter.Companion.tint(GlanceTheme.colors.onSurface),
                        modifier = GlanceModifier.Companion.size(15.dp)
                    )
                    Text(
                        text = it.label,
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 13.sp
                        )
                    )
                    if (labels.count() > 1) {
                        Text(
                            text = " +${labels.count() - 1}",
                            style = TextStyle(
                                color = GlanceTheme.colors.surfaceVariant,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                MetadataItem(
                    iconRes = CommonIcons.WAVEFORM_18,
                    count = audiosCount,
                    showSeparator = true // First item after priority
                )
                MetadataItem(
                    iconRes = CommonIcons.BELL_RING_18,
                    count = remindersCount,
                    showSeparator = audiosCount > 0 // Only show separator if previous item was shown
                )
                labels.firstOrNull()?.let { firstLabel ->
                    MetadataItem(
                        iconRes = CommonIcons.HASTAG,
                        count = labels.size, // Pass the total count of labels
                        label = firstLabel.label,
                        showSeparator = audiosCount > 0 || remindersCount > 0
                    )
                }
            }

            // Add a divider if it's not the last item
            LinearProgressIndicator(
                progress = 1f,
                color = GlanceTheme.colors.surfaceVariant,
                modifier = GlanceModifier.Companion
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(GlanceTheme.colors.surface)
            )
        }
    }

    @Composable
    private fun MetadataItem(
        iconRes: Int, // Or ImageProvider directly
        count: Int,
        label: String? = null, // For the main label text
        showSeparator: Boolean = true // To control the initial separator
    ) {
        if (count > 0) {
            if (showSeparator) {
                Text(
                    text = "|",
                    style = TextStyle(
                        color = GlanceTheme.colors.surfaceVariant,
                        fontSize = 24.sp
                    ),
                    modifier = GlanceModifier.Companion.padding(start = 6.dp, end = 6.dp)
                )
            }
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = null, // Consider adding meaningful descriptions
                colorFilter = ColorFilter.Companion.tint(GlanceTheme.colors.onSurface),
                modifier = GlanceModifier.Companion.size(if (label != null) 15.dp else 16.dp) // Adjust size if needed
            )
            label?.let {
                Text(
                    text = it,
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp
                    )
                )
            }
            if (count > 1 && label == null) { // Only show "+N" if it's not the primary label display
                Text(
                    text = " +${count - 1}",
                    style = TextStyle(
                        color = GlanceTheme.colors.surfaceVariant,
                        fontSize = 12.sp,
                    )
                )
            } else if (label != null && count > 1) { // For tags, show the count differently
                Text(
                    text = " +${count - 1}",
                    style = TextStyle(
                        color = GlanceTheme.colors.surfaceVariant,
                        fontSize = 12.sp
                    )
                )
            }
        }
    }
    private fun priority(priority: Int): Priorities {
        return when (priority) {
            1 -> Priorities.LOW
            2 -> Priorities.MED
            3 -> Priorities.HIG
            4 -> Priorities.URG
            else -> Priorities.NON
        }
    }
}