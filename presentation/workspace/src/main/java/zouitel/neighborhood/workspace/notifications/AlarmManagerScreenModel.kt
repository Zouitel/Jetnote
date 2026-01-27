package zouitel.neighborhood.workspace.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Stable
import cafe.adriel.voyager.core.model.ScreenModel

@Stable
@Suppress("DeferredResultUnused")
class AlarmManagerScreenModel(private val context: Context, receiver: NotificationReceiver): ScreenModel {
    private val intent = Intent(context , receiver::class.java)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val basicPendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    @SuppressLint("NewApi")
    fun initializeAlarm(data: String, id: Int, atTime: Long) {
        intent.putExtra("data", data)
        intent.putExtra("notification_id", id)

        scheduleAlarm(atTime, id)
    }

    @SuppressLint("NewApi")
    private fun scheduleAlarm(atTime: Long, id: Int) {
        val pendingIntent = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val isScheduleExactAlarms = alarmManager.canScheduleExactAlarms()

        if (isScheduleExactAlarms) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                atTime,
                pendingIntent ?: basicPendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                atTime,
                pendingIntent ?: basicPendingIntent
            )
        }
    }

    fun cancelAlarm(id: Int) {
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                id,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    }
}