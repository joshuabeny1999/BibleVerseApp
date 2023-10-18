package ch.joshuah.bibleverseapp.fragments

import BibleVerseApiService
import android .os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ch.joshuah.bibleverseapp.R
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VerseFragment : Fragment() {
    private val bibleVerseApiService = BibleVerseApiService()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fetchAndDisplayBibleVerse()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_verse, container, false)
    }

    @OptIn(DelicateCoroutinesApi::class)
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
                    view?.findViewById<TextView>(R.id.fragment_verse_textViewBibleVerse)?.text = bibleVerse.text
                    view?.findViewById<TextView>(R.id.fragment_verse_textViewBibleVerseReference)?.text = bibleVerse.reference
                }
            }
        }
    }
    companion object
}