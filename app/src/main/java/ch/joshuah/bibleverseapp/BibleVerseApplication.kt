package ch.joshuah.bibleverseapp

import android.app.Application
import ch.joshuah.bibleverseapp.repository.BibleVerseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BibleVerseApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val repository = BibleVerseRepository.getInstance(this)

        CoroutineScope(Dispatchers.IO).launch {
            repository.cleanUpOldBibleVerses().collect {}
        }
    }
}