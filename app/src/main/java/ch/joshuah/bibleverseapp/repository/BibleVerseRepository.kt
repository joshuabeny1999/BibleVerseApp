package ch.joshuah.bibleverseapp.repository

import ch.joshuah.bibleverseapp.services.BibleVerseApiService
import android.content.Context
import androidx.room.Room
import ch.joshuah.bibleverseapp.data.BibleVerse
import ch.joshuah.bibleverseapp.data.storage.AppDatabase
import ch.joshuah.bibleverseapp.data.storage.BibleVerseDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Date

class BibleVerseRepository(context: Context) {
    private var bibleVerseDao: BibleVerseDao
    private val bibleVerseApiService = BibleVerseApiService()

    init {
        val database = Room.databaseBuilder(
            context,
            AppDatabase::class.java, "bible_verse_app.db"
        ).build()
        bibleVerseDao = database.bibleVerseDao()
    }

    private suspend fun getAllStoredBibleVerses(): List<BibleVerse> {
        return bibleVerseDao.getAll()
    }

    private suspend fun getBibleVerseByDayAndVersion(date: Date, version: String): BibleVerse? {
        return bibleVerseDao.findByDayAndVersion(date, version)
    }

    private suspend fun insertBibleVerse(verse: BibleVerse) {
        bibleVerseDao.insert(verse)
    }

    private suspend fun deleteBibleVerse(verse: BibleVerse) {
        bibleVerseDao.delete(verse)
    }

    fun getDailyBibleVerse(date: Date, version: String): Flow<Result<BibleVerse>> = flow {
        val cachedVerse = getBibleVerseByDayAndVersion(date, version)

        if (cachedVerse != null) {
            emit(Result.success(cachedVerse))
        } else {
            bibleVerseApiService.fetchBibleVerse(version).collect { result ->
                if (result.isSuccess) {
                    val apiVerse = result.getOrNull()
                    if (apiVerse != null) {
                        insertBibleVerse(apiVerse)
                        emit(Result.success(apiVerse))
                    } else {
                        emit(Result.failure(Error("No Bible verse available from the API")))
                    }
                } else if (result.isFailure) {
                    emit(Result.failure(Error("Error fetching Bible verse from the API: ${result.exceptionOrNull()?.message}")))
                }
            }
        }
    }

    fun cleanUpOldBibleVerses(): Flow<Unit> = flow {
        val bibleVerses = getAllStoredBibleVerses()
        val today = Date()

        for (bibleVerse in bibleVerses) {
            val diff: Long = today.time - bibleVerse.date.time
            val days = diff / (24 * 60 * 60 * 1000)

            if (days > 30) {
                deleteBibleVerse(bibleVerse)
            }
        }

        emit(Unit)
    }

    companion object {
        private var instance: BibleVerseRepository? = null

        fun getInstance(context: Context): BibleVerseRepository {
            return instance ?: synchronized(this) {
                instance ?: BibleVerseRepository(context).also { instance = it }
            }
        }
    }

}
