package ch.joshuah.bibleverseapp.widgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import android.appwidget.AppWidgetManager
import java.util.Calendar

// BroadcastReceiver to trigger the widget updates
class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, BibleVerseWidget::class.java)
        )

        // Check if there are any widgets to update
        if (appWidgetIds.isNotEmpty()) {
            BibleVerseWidget.updateWidgets(context)
            Toast.makeText(context, "Bible Verse Widgets Updated", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "No widgets to update", Toast.LENGTH_SHORT).show()
        }
    }
}

// Scheduler to set up or cancel the AlarmManager
object WidgetUpdateScheduler {
    fun setupWidgetUpdateAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetUpdateReceiver::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0) // Mitternacht
            set(Calendar.MINUTE, 5)     // 5 Minuten nach Mitternacht
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelWidgetUpdateAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WidgetUpdateReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }
}