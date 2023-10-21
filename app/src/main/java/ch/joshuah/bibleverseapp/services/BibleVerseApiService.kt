import android.net.Uri
import androidx.core.text.HtmlCompat
import ch.joshuah.bibleverseapp.data.BibleVerse
import ch.joshuah.bibleverseapp.data.VotdApiResponse
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import java.io.IOException
import java.util.Date

class BibleVerseApiService {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun fetchBibleVerse(version: String): Flow<BibleVerse?> = flow {
        val apiUrl = "https://www.biblegateway.com/votd/get/?format=json&version=$version"

        val request = Request.Builder()
            .url(apiUrl)
            .get()
            .build()

        val response = try {
            client.newCall(request).await()
        } catch (e: IOException) {
            null
        }

        if (response?.isSuccessful == true) {
            val json = response.body?.string()
            val bibleVerse = parseJsonToBibleVerse(json)
            emit(bibleVerse)
        } else {
            emit(null)
        }
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWith(Result.failure(e))
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resumeWith(Result.success(response))
            }
        })

        continuation.invokeOnCancellation { cancel() }
    }

    private fun parseJsonToBibleVerse(json: String?): BibleVerse? {
        if (json.isNullOrEmpty()) {
            throw IllegalArgumentException("JSON is empty or null")
        }
        try {
            val votdApiResponse = gson.fromJson(json, VotdApiResponse::class.java)

            if (votdApiResponse.votd.text.isEmpty()) {
                throw IllegalArgumentException("JSON does not contain a Bible verse")
            }

            val text = HtmlCompat.fromHtml(votdApiResponse.votd.text, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            val reference = votdApiResponse.votd.display_ref
            val version = votdApiResponse.votd.version_id
            val versionLong = HtmlCompat.fromHtml(votdApiResponse.votd.version, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
            val link = votdApiResponse.votd.permalink

            return BibleVerse(text, reference, version, versionLong, Uri.parse(link), Date())
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
