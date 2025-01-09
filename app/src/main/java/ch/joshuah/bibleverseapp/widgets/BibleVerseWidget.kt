package ch.joshuah.bibleverseapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
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
import java.util.logging.Logger


class BibleVerseWidget : AppWidgetProvider() {

    companion object {
        fun updateWidgets(context: Context) {
            val intent = Intent(context.applicationContext, BibleVerseWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
            // since it seems the onUpdate() is only fired on that:
            // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
            // since it seems the onUpdate() is only fired on that:
            val widgetManager = AppWidgetManager.getInstance(context)
            val ids = widgetManager.getAppWidgetIds(
                ComponentName(
                    context,
                    BibleVerseWidget::class.java
                )
            )

            widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list)

            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }
    }
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Start the alarm for widget updates
        Logger.getLogger("BibleVerseWidget").info("Widget update enabled")
        WidgetUpdateScheduler.setupWidgetUpdateAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Stop the alarm when no widgets are active
        WidgetUpdateScheduler.cancelWidgetUpdateAlarm(context)
    }
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // There may be multiple widgets active, so update all of them
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val coroutineScope = CoroutineScope(Dispatchers.IO)

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val textColor = preferences.getInt("widget_color", -1)


        coroutineScope.launch {
            val bibleVerse = fetchBibleVerse(context, getVersion(context)) ?: return@launch

            // Construct the RemoteViews object
            val views = RemoteViews(context.packageName, R.layout.bible_verse_widget)
            val referenceAndVersion = "${bibleVerse.reference} (${bibleVerse.versionLong})"
            views.setTextViewText(R.id.widget_verse_textViewBibleVerse, bibleVerse.text)
            views.setTextViewText(
                R.id.widget_verse_textViewBibleVerseReference,
                referenceAndVersion
            )

            views.setTextColor(R.id.widget_verse_textViewBibleVerse, textColor)
            views.setTextColor(R.id.widget_verse_textViewBibleVerseReference, textColor)

            val intent = Intent(context, MainActivity::class.java)
            intent.action = "ch.joshuah.bibleverseapp.UPDATE_WIDGETS"
            val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_verse, pendingIntent)


            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private suspend fun fetchBibleVerse(context: Context, version: String): BibleVerse? {
        val bibleVerseRepository = BibleVerseRepository.getInstance(context)
        val result = bibleVerseRepository.getDailyBibleVerse(Date(), version).first()
        if (result.isSuccess) {
            return result.getOrNull()
        } else {
            throw Exception("No Bible verse available")
        }
    }

    private fun getVersion(context: Context): String {
        val defaultVersion =
            context.getString(R.string.preference_listPreference_bible_version_default_value)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString("bible_version", defaultVersion) ?: defaultVersion
    }
}


