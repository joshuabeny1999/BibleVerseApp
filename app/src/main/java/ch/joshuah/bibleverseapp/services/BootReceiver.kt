package ch.joshuah.bibleverseapp.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val notificationsEnabled = sharedPrefs.getBoolean("enable_notifications", false)
            if (notificationsEnabled) {
                DailyVerseService().setNotificationAlarm(context, sharedPrefs)
            }
        }
    }
}
