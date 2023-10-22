package ch.joshuah.bibleverseapp.fragments

import android.content.Context
import android.content.Intent
import android .os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import ch.joshuah.bibleverseapp.R
import ch.joshuah.bibleverseapp.data.BibleVerse
import ch.joshuah.bibleverseapp.repository.BibleVerseRepository
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Date

class VerseFragment : Fragment() {
    private lateinit var bibleVerseRepository: BibleVerseRepository

    private lateinit var verseText: TextView
    private lateinit var verseReference: TextView
    private lateinit var shareButton: Button

    private var bibleVerse : BibleVerse? = null


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
        shareButton = view.findViewById(R.id.fragment_verse_buttonShare)

        fetchAndDisplayBibleVerse(getVersion(requireContext()))

        shareButton.setOnClickListener {
            shareBibleVerse(requireContext())
        }
    }

    private fun shareBibleVerse(context: Context) {
        if (bibleVerse != null) {
            val sendIntent = Intent().apply {
                val referenceAndVersion = "${bibleVerse!!.reference} (${bibleVerse!!.versionLong})"
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "${bibleVerse!!.text} \n (${referenceAndVersion}) \n\n ${bibleVerse!!.link}")
                type = "text/plain"
            }

            val shareIntent = Intent.createChooser(sendIntent, context.getString(R.string.fragment_verse_share_chooser_title))
            startActivity(shareIntent)
        }
    }

    private fun fetchAndDisplayBibleVerse(version: String) {
        lifecycleScope.launch {
            try {
                bibleVerseRepository.getDailyBibleVerse(Date(), version)
                    .collect { result ->
                        if (result.isSuccess) {
                            val collectedBibleVerse = result.getOrNull()
                            if (collectedBibleVerse != null) {
                                 bibleVerse = collectedBibleVerse
                                val referenceAndVersion = "${bibleVerse!!.reference} (${bibleVerse!!.versionLong})"
                                verseText.text = bibleVerse!!.text
                                verseReference.text = referenceAndVersion
                            } else {
                                showNoVerseError(requireContext())
                            }
                        } else {
                            if (result.exceptionOrNull()?.message == "No Bible verse available from the API") {
                                showNoVerseError(requireContext())
                            } else {
                                showNetworkError(requireContext())
                            }
                        }
                    }
            } catch (e: Exception) {
                showNetworkError(requireContext())
            }
        }
    }

    private fun getVersion(context : Context) : String {
        val defaultVersion = context.getString(R.string.preference_listPreference_bible_version_default_value)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return preferences.getString("bible_version", defaultVersion) ?: defaultVersion
    }

    private fun showNoVerseError(context : Context) {
        Snackbar.make(verseText, context.getString(R.string.fragment_verse_error_no_verse), Snackbar.LENGTH_LONG).show()
    }

    private fun showNetworkError(context : Context) {
        Snackbar.make(verseText, context.getString(R.string.fragment_verse_error_no_internet), Snackbar.LENGTH_LONG).show()
    }

}