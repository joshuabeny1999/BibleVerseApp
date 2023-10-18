import androidx.core.text.HtmlCompat
import ch.joshuah.bibleverseapp.data.BibleVerse
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class BibleVerseApiService {
    fun fetchBibleVerse(version: String): BibleVerse? {
        val apiUrl = "https://www.biblegateway.com/votd/get/?format=json&version=$version"

        try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val inputStream = connection.inputStream
                val reader = BufferedReader(InputStreamReader(inputStream))
                val json = reader.readText()

                val bibleVerse = parseJsonToBibleVerse(json)

                reader.close()
                return bibleVerse
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun parseJsonToBibleVerse(json: String): BibleVerse {
        val jsonObject = JSONObject(json)
        val votdObject = jsonObject.getJSONObject("votd")
        val text = HtmlCompat.fromHtml(votdObject.getString("text"), HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
        val reference = votdObject.getString("display_ref")
        val version = votdObject.getString("version_id")
        val versionLong = votdObject.getString("version")
        val link = votdObject.getString("permalink")

        return BibleVerse(text, reference, version, versionLong, link)
    }
}