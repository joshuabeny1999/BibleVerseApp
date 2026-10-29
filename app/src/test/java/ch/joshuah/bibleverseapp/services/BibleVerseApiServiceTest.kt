package ch.joshuah.bibleverseapp.services

import android.net.Uri
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.bibles.api.BiblesApi
import com.youversion.platform.core.bibles.models.BiblePassage
import com.youversion.platform.core.bibles.models.BibleVersion
import com.youversion.platform.core.verseimages.api.VerseImagesApi
import com.youversion.platform.core.verseimages.models.VerseImage
import com.youversion.platform.core.votd.api.VotdApi
import com.youversion.platform.core.votd.models.YouVersionVerseOfTheDay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

class BibleVerseApiServiceTest {

    private val votdApi: VotdApi = mock()
    private val biblesApi: BiblesApi = mock()
    private val verseImagesApi: VerseImagesApi = mock()
    private val mockUri: Uri = mock()
    private val service = BibleVerseApiService(
        votdApi = votdApi,
        biblesApi = biblesApi,
        verseImagesApi = verseImagesApi,
        uriParser = { mockUri }
    )

    @Test
    fun fetchBibleVerse_resolvesVerseImageArtworkAndAttribution() = runBlocking {
        val versionId = 111
        val votd = YouVersionVerseOfTheDay(day = 1, passageUsfm = "JHN.3.16")
        val passage = BiblePassage(
            id = "JHN.3.16",
            content = "<p><span>13</span>For God so loved the world that he gave his one and only Son.</p>",
            reference = "John 3:16"
        )
        val bibleVersion = BibleVersion(
            id = versionId,
            title = "New International Version",
            abbreviation = "NIV",
            promotionalContent = "",
            copyright = "Public Domain",
            languageTag = "en",
            localizedAbbreviation = "NIV",
            localizedTitle = "New International Version",
            readerFooter = "",
            readerFooterUrl = "",
            bookCodes = emptyList(),
            books = emptyList(),
            textDirection = "ltr",
            organizationId = "1"
        )
        val verseImage = VerseImage(
            id = "img123",
            imageUrl = "https://images.bible.com/verse_image_123.jpg",
            passageId = "JHN.3.16",
            attribution = "Photo by Unsplash / YouVersion",
            languageTag = "en",
            width = 1200,
            height = 1200,
            aspectRatio = 1.0
        )
        val imageResponse = mock<PaginatedResponse<VerseImage>>()
        whenever(imageResponse.data).thenReturn(listOf(verseImage))

        whenever(votdApi.verseOfTheDay(anyOrNull())).thenReturn(votd)
        whenever(biblesApi.passage(eq(versionId), eq("JHN.3.16"), anyOrNull())).thenReturn(passage)
        whenever(biblesApi.version(eq(versionId))).thenReturn(bibleVersion)
        whenever(verseImagesApi.verseImages(eq("JHN.3.16"), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(imageResponse)

        val result = service.fetchBibleVerse("111").first()

        assertTrue("Expected success result: ${result.exceptionOrNull()}", result.isSuccess)
        val verse = result.getOrThrow()
        assertEquals("For God so loved the world that he gave his one and only Son.", verse.text)
        assertEquals("John 3:16", verse.reference)
        assertEquals("111", verse.version)
        assertEquals("New International Version", verse.versionLong)
        assertEquals(mockUri, verse.link)
        assertEquals("https://images.bible.com/verse_image_123.jpg", verse.imageUrl)
        assertEquals("Photo by Unsplash / YouVersion", verse.attribution)
    }

    @Test
    fun fetchBibleVerse_fallbackWhenVerseImageFailsOrNull() = runBlocking {
        val versionId = 111
        val votd = YouVersionVerseOfTheDay(day = 1, passageUsfm = "JHN.3.16")
        val passage = BiblePassage(
            id = "JHN.3.16",
            content = "<p>For God so loved the world that he gave his one and only Son.</p>",
            reference = "John 3:16"
        )

        whenever(votdApi.verseOfTheDay(anyOrNull())).thenReturn(votd)
        whenever(biblesApi.passage(eq(versionId), eq("JHN.3.16"), anyOrNull())).thenReturn(passage)
        whenever(verseImagesApi.verseImages(eq("JHN.3.16"), anyOrNull(), anyOrNull(), anyOrNull())).thenAnswer { throw IOException("Image API unavailable") }

        val result = service.fetchBibleVerse("111").first()

        assertTrue("Expected success result even when verse image fails: ${result.exceptionOrNull()}", result.isSuccess)
        val verse = result.getOrThrow()
        assertEquals("John 3:16", verse.reference)
        assertNull("imageUrl should be null on fallback", verse.imageUrl)
        assertNull("attribution should be null on fallback", verse.attribution)
    }

    @Test
    fun fetchBibleVerse_fallbackVersionWhenStringNotInt() = runBlocking {
        val defaultVersionId = BibleDefaults.VERSION_ID
        val votd = YouVersionVerseOfTheDay(day = 1, passageUsfm = "GEN.1.1")
        val passage = BiblePassage(
            id = "GEN.1.1",
            content = "1 In the beginning God created the heavens and the earth.",
            reference = "Genesis 1:1"
        )
        val emptyImageResponse = mock<PaginatedResponse<VerseImage>>()
        whenever(emptyImageResponse.data).thenReturn(emptyList())

        whenever(votdApi.verseOfTheDay(anyOrNull())).thenReturn(votd)
        whenever(biblesApi.passage(eq(defaultVersionId), eq("GEN.1.1"), anyOrNull())).thenReturn(passage)
        whenever(verseImagesApi.verseImages(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(emptyImageResponse)

        val result = service.fetchBibleVerse("NIV").first()

        assertTrue("Expected success result but was failure: ${result.exceptionOrNull()}", result.isSuccess)
        val verse = result.getOrThrow()
        assertEquals("In the beginning God created the heavens and the earth.", verse.text)
        assertEquals("Genesis 1:1", verse.reference)
        assertEquals(defaultVersionId.toString(), verse.version)
    }

    @Test
    fun fetchBibleVerse_emptyPassageUsfm() = runBlocking {
        val votd = YouVersionVerseOfTheDay(day = 1, passageUsfm = "")

        whenever(votdApi.verseOfTheDay(anyOrNull())).thenReturn(votd)

        val result = service.fetchBibleVerse("111").first()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun fetchBibleVerse_sdkThrowsException() = runBlocking {
        whenever(votdApi.verseOfTheDay(anyOrNull())).thenAnswer { throw IOException("Network error") }

        val result = service.fetchBibleVerse("111").first()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
