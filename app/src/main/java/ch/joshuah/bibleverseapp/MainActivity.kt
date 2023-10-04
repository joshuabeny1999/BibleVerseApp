package ch.joshuah.bibleverseapp

import BibleVerseApiService
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val bibleVerseApiService = BibleVerseApiService()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Fetch bible ferse and replace text in TextView
        fetchAndDisplayBibleVerse()
    }

    private fun fetchAndDisplayBibleVerse() {
        GlobalScope.launch(Dispatchers.IO) {
            // Debug log
            println("Fetching bible verse...")
            // Hier rufen wir den API-Dienst auf und holen den Bibelvers
            val bibleVerse = bibleVerseApiService.fetchBibleVerse("LUTH1545")
            // Debug log
            println("Fetched bible verse: $bibleVerse")

            // Den erhaltenen Bibelvers auf der UI anzeigen
            withContext(Dispatchers.Main) {
                bibleVerse?.let {
                    // Angenommen, die TextView hat die ID "textViewBibleVerse" in deinem Layout
                    // Dann kannst du den Bibelvers so anzeigen:
                    findViewById<TextView>(R.id.textViewBibleVerse).text = bibleVerse.text
                    findViewById<TextView>(R.id.textViewBibleVerseReference).text = bibleVerse.reference
                }
            }
        }
    }
}