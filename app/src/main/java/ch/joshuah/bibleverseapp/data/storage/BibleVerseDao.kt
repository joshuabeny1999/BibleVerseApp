package ch.joshuah.bibleverseapp.data.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import ch.joshuah.bibleverseapp.data.BibleVerse
import java.util.Date

@Dao
interface BibleVerseDao {
    @Query("SELECT * FROM verses")
    suspend fun getAll(): List<BibleVerse>

    @Query("SELECT * FROM verses WHERE date / (1000 /* drop millis*/ * 60 /* drop seconds */ * 60 /* drop minutes */ * 24 /* drop hours */) = :date / 86400000 AND version LIKE :version LIMIT 1")
    suspend fun findByDayAndVersion(date: Date, version: String): BibleVerse?

    @Insert
    suspend fun insert(verse: BibleVerse)

    @Delete
    suspend fun delete(verse: BibleVerse)
}