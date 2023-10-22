package ch.joshuah.bibleverseapp.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import ch.joshuah.bibleverseapp.MainActivity
import ch.joshuah.bibleverseapp.R
import ch.joshuah.bibleverseapp.data.BibleVerse
import ch.joshuah.bibleverseapp.repository.BibleVerseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val channelId = intent.getStringExtra("channelId") ?: "defaultChannelId"

        println("NOTIFICATION DEBUG: Received notification broadcast")
        println("NOTIFICATION DEBUG: channel id is $channelId")
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        coroutineScope.launch {
            println("NOTIFICATION DEBUG: Fetching bible verse")
            val bibleVerse = fetchBibleVerse(context)
            if (bibleVerse != null) {
                println("NOTIFICATION DEBUG: could fetch bible verse")
                val referenceAndVersion = "${bibleVerse.reference} (${bibleVerse.versionLong})"
                createNotification(context, "${bibleVerse.text}\n\n$referenceAndVersion", channelId)
            } else {
                println("NOTIFICATION DEBUG: could not fetch bible verse")
            }
        }
    }

    private suspend fun fetchBibleVerse(context: Context): BibleVerse? {
        val bibleVerseRepository = BibleVerseRepository.getInstance(context)

        return try {
            val result = bibleVerseRepository.getDailyBibleVerse(Date(), getVersion(context))
                .first { it.isSuccess }
                result.getOrNull()
        } catch (e: Exception) {
            println("NOTIFICATION DEBUG: Could not fetch bible verse CATCH")
            println(e)
            null
        }
    }



    private fun createNotification(context: Context, bibleVerse: String, channelId: String) {
        println("NOTIFICATION DEBUG: Creating notification")
        println("NOTIFICATION DEBUG: bible verse is $bibleVerse")
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val resultIntent = Intent(context, MainActivity::class.java)
        val resultPendingIntent = PendingIntent.getActivity(
            context,
            0,
            resultIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.bigText(bibleVerse)

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_text_snippet_24)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(bibleVerse)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(resultPendingIntent)
            .setStyle(bigTextStyle)

        val notificationId = 1
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    private fun getVersion(context : Context) : String {
        val defaultVersion = context.getString(R.string.preference_listPreference_bible_version_default_value)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString("bible_version", defaultVersion) ?: defaultVersion
    }

}
