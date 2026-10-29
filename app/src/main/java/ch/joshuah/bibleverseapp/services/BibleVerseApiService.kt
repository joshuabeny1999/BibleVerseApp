package ch.joshuah.bibleverseapp.services

import android.net.Uri
import androidx.core.text.HtmlCompat
import ch.joshuah.bibleverseapp.data.BibleVerse
import com.youversion.platform.core.BibleDefaults
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.api.BiblesApi
import com.youversion.platform.core.verseimages.api.VerseImagesApi
import com.youversion.platform.core.votd.api.VotdApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date

class BibleVerseApiService(
    private val votdApi: VotdApi = YouVersionApi.votd,
    private val biblesApi: BiblesApi = YouVersionApi.bible,
    private val verseImagesApi: VerseImagesApi = YouVersionApi.verseImages,
    private val uriParser: (String) -> Uri = { url ->
        try {
            Uri.parse(url) ?: Uri.EMPTY
        } catch (e: Throwable) {
            Uri.EMPTY
        }
    }
) {
    suspend fun fetchBibleVerse(version: String): Flow<Result<BibleVerse>> = flow {
        try {
            val versionId = version.toIntOrNull() ?: BibleDefaults.VERSION_ID

            val votd = votdApi.verseOfTheDay(versionId)
            if (votd.passageUsfm.isBlank()) {
                emit(Result.failure(IllegalArgumentException("VOTD passage is empty or null")))
                return@flow
            }

            val passage = biblesApi.passage(versionId = versionId, passageId = votd.passageUsfm)
            if (passage.content.isBlank()) {
                emit(Result.failure(IllegalArgumentException("Passage content is empty")))
                return@flow
            }

            val bibleVersion = try {
                biblesApi.version(versionId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }

            val verseImage = try {
                verseImagesApi.verseImage(votd.passageUsfm)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }

            val cleanText = stripHtml(passage.content)
            val reference = passage.reference.ifBlank { votd.passageUsfm }
            val versionStr = versionId.toString()
            val versionLong = bibleVersion?.localizedTitle ?: bibleVersion?.title ?: "Bible"
            val shareUrlStr = "https://www.bible.com/bible/$versionId/${votd.passageUsfm}"
            val linkUri = uriParser(shareUrlStr)

            val bibleVerse = BibleVerse(
                text = cleanText,
                reference = reference,
                version = versionStr,
                versionLong = versionLong,
                link = linkUri,
                date = Date(),
                imageUrl = verseImage?.imageUrl,
                attribution = verseImage?.attribution
            )

            emit(Result.success(bibleVerse))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    private fun stripHtml(html: String): String {
        return try {
            HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (e: Throwable) {
            html.replace(Regex("<[^>]*>"), "").trim()
        }
    }
}
