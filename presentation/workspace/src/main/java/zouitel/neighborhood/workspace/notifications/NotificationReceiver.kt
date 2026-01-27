package zouitel.neighborhood.workspace.notifications

import android.R
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.graphics.BitmapFactory
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import zouitel.neighborhood.workspace.WorkplaceActivity
import zouitel.neighborhood.workspace.mapper.JsonInstant
import zouitel.neighborhood.workspace.model.Data
import org.koin.core.component.KoinComponent

class NotificationReceiver: BroadcastReceiver(), KoinComponent {
    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("NewApi")
    @CallSuper
    override fun onReceive(context: Context, intent: Intent) {
        val notifyManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val jsonNote = intent.getStringExtra("data")
        val note = JsonInstant.decodeFromString<Data>(jsonNote ?: return)

        val publicChannel = NotificationChannel(
            note.uid,
            "jetnote_notification_channel",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminder notification"
            enableVibration(true)
            enableLights(true)
            vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setShowBadge(true)
        }
        notifyManager.createNotificationChannel(publicChannel)

        // Use action string instead of class reference
        val activityIntent = Intent(context, WorkplaceActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("start_workplace", true)
            putExtra("data", jsonNote)
            // Set package to ensure it opens your app
            setPackage(context.packageName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            note.uid.hashCode(),
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifyBuilder = NotificationCompat.Builder(context, note.uid)
            .setStyle(
                NotificationCompat
                    .BigPictureStyle().bigPicture(
                    note.medias.firstOrNull()?.uri?.let {
                        BitmapFactory.decodeFile(it)
                    }
                ).bigLargeIcon(null as Bitmap?)
            )
            .setLargeIcon(
                note.medias.firstOrNull()?.uri?.let {
                    BitmapFactory.decodeFile(it)
                }
            )
            .setSmallIcon(R.drawable.ic_popup_reminder)
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setContentTitle(note.title)
            .setContentText(note.description)
            .build()

        notifyManager.notify(
            note.uid.hashCode(),
            notifyBuilder
        )
    }
}