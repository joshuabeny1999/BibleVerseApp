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
            val dayOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)

            val votd = votdApi.verseOfTheDay(dayOfYear)
            if (votd.passageUsfm.isBlank()) {
                emit(Result.failure(IllegalArgumentException("VOTD passage is empty or null")))
                return@flow
            }

            // Fetch passage in plain text format directly from YouVersion API
            val passage = biblesApi.passage(
                versionId = versionId,
                passageId = votd.passageUsfm,
                format = "text"
            )
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
                val langTag = bibleVersion?.languageTag?.takeIf { it.isNotBlank() } ?: "en"
                var response = verseImagesApi.verseImages(
                    passageId = votd.passageUsfm,
                    languageRanges = listOf(langTag),
                    pageSize = 5
                )
                if (response.data.isEmpty()) {
                    response = verseImagesApi.verseImages(
                        passageId = votd.passageUsfm,
                        languageRanges = listOf("en", "*"),
                        pageSize = 5
                    )
                }
                if (response.data.isEmpty()) {
                    response = verseImagesApi.verseImages(
                        passageId = votd.passageUsfm,
                        languageRanges = emptyList(),
                        pageSize = 5
                    )
                }
                response.data.firstOrNull()
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
        val rawText = try {
            HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (e: Throwable) {
            html.replace(Regex("<[^>]*>"), "").trim()
        }
        return sanitizeVerseText(rawText)
    }

    companion object {
        fun sanitizeVerseText(text: String): String {
            return text.replace(Regex("(?m)^\\d+\\s*(?=[\\p{L}])"), "")
                       .replace(Regex("(?m)^\\d+[a-zA-Z]?\\s*"), "")
                       .trim()
        }
    }
}
