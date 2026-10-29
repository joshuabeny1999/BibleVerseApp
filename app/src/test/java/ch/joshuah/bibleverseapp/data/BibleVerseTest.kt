package ch.joshuah.bibleverseapp.data

import android.net.Uri
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.mock
import java.util.Date

class BibleVerseTest {

    @Test
    fun bibleVerse_defaultImageAndAttributionAreNull() {
        val mockUri: Uri = mock()
        val date = Date()
        val verse = BibleVerse(
            text = "For God so loved the world",
            reference = "John 3:16",
            version = "111",
            versionLong = "NIV",
            link = mockUri,
            date = date
        )

        assertEquals("For God so loved the world", verse.text)
        assertEquals("John 3:16", verse.reference)
        assertEquals("111", verse.version)
        assertEquals("NIV", verse.versionLong)
        assertEquals(mockUri, verse.link)
        assertEquals(date, verse.date)
        assertNull("imageUrl should default to null", verse.imageUrl)
        assertNull("attribution should default to null", verse.attribution)
    }

    @Test
    fun bibleVerse_withImageUrlAndAttribution() {
        val mockUri: Uri = mock()
        val date = Date()
        val verse = BibleVerse(
            text = "In the beginning",
            reference = "Genesis 1:1",
            version = "111",
            versionLong = "NIV",
            link = mockUri,
            date = date,
            imageUrl = "https://images.bible.com/verse_image_123.jpg",
            attribution = "Photo by Unsplash / YouVersion"
        )

        assertEquals("https://images.bible.com/verse_image_123.jpg", verse.imageUrl)
        assertEquals("Photo by Unsplash / YouVersion", verse.attribution)
    }
}
