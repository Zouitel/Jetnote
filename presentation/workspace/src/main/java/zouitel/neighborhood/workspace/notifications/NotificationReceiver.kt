package zouitel.neighborhood.workspace.notifications

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import zouitel.neighborhood.workspace.WorkplaceActivity
import zouitel.neighborhood.workspace.mapper.JsonInstant
import zouitel.neighborhood.workspace.model.Data
import org.koin.core.component.KoinComponent

class NotificationReceiver : BroadcastReceiver(), KoinComponent {
    override fun onReceive(context: Context, intent: Intent) {
        val json = intent.getStringExtra(EXTRA_DATA) ?: return
        val note = runCatching { JsonInstant.decodeFromString<Data>(json) }.getOrNull() ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Jetnote reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for your Jetnote notes"
                enableVibration(true)
                vibrationPattern = longArrayOf(500, 500, 500)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            }
            manager.createNotificationChannel(channel)
        }

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, note.uid.hashCode())
        val openIntent = Intent(context, WorkplaceActivity::class.java).apply {
            action = ACTION_OPEN_WORKPLACE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_START_WORKPLACE, true)
            putExtra(EXTRA_DATA, json)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentTitle(note.title.ifBlank { "Jetnote reminder" })
            .setContentText("Tap to open your note")
            .build()
        manager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "workspace_reminders"
        const val EXTRA_DATA = "data"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_START_WORKPLACE = "start_workplace"
        const val ACTION_OPEN_WORKPLACE = "city.zouitel.action.OPEN_WORKPLACE"
    }
}
