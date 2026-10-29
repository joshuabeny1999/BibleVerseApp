package ch.joshuah.bibleverseapp.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.view.View
import android.widget.RemoteViews
import androidx.preference.PreferenceManager
import ch.joshuah.bibleverseapp.MainActivity
import ch.joshuah.bibleverseapp.R
import ch.joshuah.bibleverseapp.data.BibleVerse
import ch.joshuah.bibleverseapp.repository.BibleVerseRepository
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import java.util.logging.Logger

fun interface BitmapFetcher {
    suspend fun fetchBitmap(context: Context, url: String): Bitmap?
}

object CoilBitmapFetcher : BitmapFetcher {
    override suspend fun fetchBitmap(context: Context, url: String): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val result = context.imageLoader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as? BitmapDrawable)?.bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}

class BibleVerseImageWidget : AppWidgetProvider() {

    companion object {
        fun updateWidgets(context: Context) {
            val intent = Intent(context.applicationContext ?: context, BibleVerseImageWidget::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            val widgetManager = try { AppWidgetManager.getInstance(context) } catch (e: Exception) { null }
            if (widgetManager != null) {
                val ids = widgetManager.getAppWidgetIds(
                    ComponentName(
                        context,
                        BibleVerseImageWidget::class.java
                    )
                )

                try {
                    widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list)
                } catch (e: Exception) {
                    // Ignore
                }

                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Logger.getLogger("BibleVerseImageWidget").info("Image widget update enabled")
        WidgetUpdateScheduler.setupWidgetUpdateAlarm(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        WidgetUpdateScheduler.cancelWidgetUpdateAlarm(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        repository: BibleVerseRepository? = null,
        bitmapFetcher: BitmapFetcher = CoilBitmapFetcher,
        coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    ) {
        coroutineScope.launch {
            try {
                val verseRepository = repository ?: BibleVerseRepository.getInstance(context)
                val bibleVerse = fetchBibleVerse(context, verseRepository, getVersion(context)) ?: return@launch

                val views = RemoteViews(context.packageName, R.layout.bible_verse_image_widget)
                val referenceAndVersion = "${bibleVerse.reference} (${bibleVerse.versionLong})"
                views.setTextViewText(R.id.widget_image_textViewReference, referenceAndVersion)

                val attribution = bibleVerse.attribution
                if (!attribution.isNullOrBlank()) {
                    views.setTextViewText(R.id.widget_image_textViewAttribution, attribution)
                    views.setViewVisibility(R.id.widget_image_textViewAttribution, View.VISIBLE)
                } else {
                    views.setViewVisibility(R.id.widget_image_textViewAttribution, View.GONE)
                }

                val imageUrl = bibleVerse.imageUrl
                if (!imageUrl.isNullOrBlank()) {
                    val bitmap = bitmapFetcher.fetchBitmap(context, imageUrl)
                    if (bitmap != null) {
                        views.setImageViewBitmap(R.id.widget_image_imageViewArtwork, bitmap)
                        views.setViewVisibility(R.id.widget_image_imageViewArtwork, View.VISIBLE)
                    } else {
                        views.setImageViewResource(R.id.widget_image_imageViewArtwork, R.drawable.app_widget_background)
                        views.setViewVisibility(R.id.widget_image_imageViewArtwork, View.VISIBLE)
                    }
                } else {
                    views.setImageViewResource(R.id.widget_image_imageViewArtwork, R.drawable.app_widget_background)
                    views.setViewVisibility(R.id.widget_image_imageViewArtwork, View.VISIBLE)
                }

                val intent = Intent(context, MainActivity::class.java)
                intent.action = "ch.joshuah.bibleverseapp.UPDATE_WIDGETS"
                val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)

                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                Logger.getLogger("BibleVerseImageWidget").warning("Failed to update widget $appWidgetId: ${e.message}")
            }
        }
    }

    private suspend fun fetchBibleVerse(
        context: Context,
        repository: BibleVerseRepository,
        version: String
    ): BibleVerse? {
        val result = repository.getDailyBibleVerse(Date(), version).first()
        return if (result.isSuccess) {
            result.getOrNull()
        } else {
            null
        }
    }

    private fun getVersion(context: Context): String {
        val defaultVersion =
            try {
                context.getString(R.string.preference_listPreference_bible_version_default_value)
            } catch (e: Exception) {
                "111"
            }
        val preferences = try {
            PreferenceManager.getDefaultSharedPreferences(context)
        } catch (e: Exception) {
            null
        }
        return preferences?.getString("bible_version", defaultVersion) ?: defaultVersion
    }
}
