package ch.joshuah.bibleverseapp.fragments

import android.content.Context
import android .os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import ch.joshuah.bibleverseapp.R
import ch.joshuah.bibleverseapp.repository.BibleVerseRepository
import kotlinx.coroutines.launch
import java.util.Date

class VerseFragment : Fragment() {
    private lateinit var bibleVerseRepository: BibleVerseRepository

    private lateinit var verseText: TextView
    private lateinit var verseReference: TextView


    override fun onAttach(context: Context) {
        super.onAttach(context)
        bibleVerseRepository = BibleVerseRepository.getInstance(requireContext())
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_verse, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        verseText = view.findViewById(R.id.fragment_verse_textViewBibleVerse)
        verseReference = view.findViewById(R.id.fragment_verse_textViewBibleVerseReference)

        fetchAndDisplayBibleVerse(getVersion(requireContext()))
    }

    private fun fetchAndDisplayBibleVerse(version: String) {
        lifecycleScope.launch {
            bibleVerseRepository.getDailyBibleVerse(Date(), version)
                .collect { bibleVerse ->
                    val referenceAndVersion = "${bibleVerse?.reference} (${bibleVerse?.versionLong})"
                    verseText.text = bibleVerse?.text
                    verseReference.text = referenceAndVersion
                    // TODO: If no verse returned by API show error, maybe if no internet or so and nothing in store.
                }
        }
    }

    private fun getVersion(context : Context) : String {
        val defaultVersion = context.getString(R.string.preference_listPreference_bible_version_default_value)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString("bible_version", defaultVersion) ?: defaultVersion
    }

}