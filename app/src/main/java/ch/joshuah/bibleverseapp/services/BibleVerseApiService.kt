

import androidx.core.text.HtmlCompat
import ch.joshuah.bibleverseapp.data.BibleVerse
import ch.joshuah.bibleverseapp.data.VotdApiResponse
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

import java.io.IOException


class BibleVerseApiService {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun fetchBibleVerse(version: String, callback: (BibleVerse?) -> Unit){
        val apiUrl = "https://www.biblegateway.com/votd/get/?format=json&version=$version"

        val request = Request.Builder()
            .url(apiUrl)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val bibleVerse = parseJsonToBibleVerse(json)
                    callback(bibleVerse)
                } else {
                    callback(null)
                }
            }
        })
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
            val versionLong = votdApiResponse.votd.version
            val link = votdApiResponse.votd.permalink

            return BibleVerse(text, reference, version, versionLong, link)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}