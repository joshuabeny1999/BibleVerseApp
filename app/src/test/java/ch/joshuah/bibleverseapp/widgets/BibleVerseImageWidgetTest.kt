package ch.joshuah.bibleverseapp.widgets

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import ch.joshuah.bibleverseapp.data.BibleVerse
import ch.joshuah.bibleverseapp.repository.BibleVerseRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

class BibleVerseImageWidgetTest {

    private lateinit var context: Context
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var repository: BibleVerseRepository
    private lateinit var mockBitmap: Bitmap

    @Before
    fun setUp() {
        context = mock()
        appWidgetManager = mock()
        repository = mock()
        mockBitmap = mock()

        whenever(context.packageName).thenReturn("ch.joshuah.bibleverseapp")
        whenever(context.getString(any())).thenReturn("111")
    }

    @Test
    fun updateAppWidget_fetchesVerseAndImage_updatesAppWidget() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val mockUri: Uri = mock()
        val verse = BibleVerse(
            text = "For God so loved the world",
            reference = "John 3:16",
            version = "111",
            versionLong = "NIV",
            link = mockUri,
            date = Date(),
            imageUrl = "https://example.com/verse.jpg",
            attribution = "Photo by Unsplash"
        )

        whenever(repository.getDailyBibleVerse(any(), any())).thenReturn(flowOf(Result.success(verse)))

        val mockBitmapFetcher = BitmapFetcher { _, url ->
            if (url == "https://example.com/verse.jpg") mockBitmap else null
        }

        val widget = BibleVerseImageWidget()
        val appWidgetId = 42

        widget.updateAppWidget(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetId = appWidgetId,
            repository = repository,
            bitmapFetcher = mockBitmapFetcher,
            coroutineScope = testScope
        )

        testScheduler.advanceUntilIdle()

        verify(appWidgetManager).updateAppWidget(eq(appWidgetId), any())
    }

    @Test
    fun updateAppWidget_verseWithoutImage_updatesAppWidgetSafely() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val mockUri: Uri = mock()
        val verse = BibleVerse(
            text = "In the beginning",
            reference = "Genesis 1:1",
            version = "111",
            versionLong = "NIV",
            link = mockUri,
            date = Date(),
            imageUrl = null,
            attribution = null
        )

        whenever(repository.getDailyBibleVerse(any(), any())).thenReturn(flowOf(Result.success(verse)))

        val mockBitmapFetcher = BitmapFetcher { _, _ -> null }

        val widget = BibleVerseImageWidget()
        val appWidgetId = 101

        widget.updateAppWidget(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetId = appWidgetId,
            repository = repository,
            bitmapFetcher = mockBitmapFetcher,
            coroutineScope = testScope
        )

        testScheduler.advanceUntilIdle()

        verify(appWidgetManager).updateAppWidget(eq(appWidgetId), any())
    }

    @Test
    fun updateAppWidget_imageFetchFails_updatesAppWidgetSafely() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)

        val mockUri: Uri = mock()
        val verse = BibleVerse(
            text = "Trust in the Lord",
            reference = "Proverbs 3:5",
            version = "111",
            versionLong = "NIV",
            link = mockUri,
            date = Date(),
            imageUrl = "https://example.com/failed.jpg",
            attribution = "YouVersion"
        )

        whenever(repository.getDailyBibleVerse(any(), any())).thenReturn(flowOf(Result.success(verse)))

        // Fetcher returns null to simulate network error loading bitmap
        val mockBitmapFetcher = BitmapFetcher { _, _ -> null }

        val widget = BibleVerseImageWidget()
        val appWidgetId = 202

        widget.updateAppWidget(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetId = appWidgetId,
            repository = repository,
            bitmapFetcher = mockBitmapFetcher,
            coroutineScope = testScope
        )

        testScheduler.advanceUntilIdle()

        verify(appWidgetManager).updateAppWidget(eq(appWidgetId), any())
    }

    @Test
    fun updateWidgets_sendsBroadcastIntent() {
        val appContext: Context = mock()
        whenever(context.applicationContext).thenReturn(appContext)

        // Verify updateWidgets can be invoked without throwing exceptions
        BibleVerseImageWidget.updateWidgets(context)

        verify(context).sendBroadcast(any())
    }
}
