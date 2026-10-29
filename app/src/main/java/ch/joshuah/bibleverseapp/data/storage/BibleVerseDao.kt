package ch.joshuah.bibleverseapp.data.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.joshuah.bibleverseapp.data.BibleVerse
import java.util.Date

@Dao
interface BibleVerseDao {
    @Query("SELECT * FROM verses")
    suspend fun getAll(): List<BibleVerse>

    @Query("SELECT * FROM verses WHERE date / (1000 * 60 * 60 * 24) = :date / 86400000 AND version = :version ORDER BY uid DESC LIMIT 1")
    suspend fun findByDayAndVersion(date: Date, version: String): BibleVerse?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(verse: BibleVerse)

    @Query("DELETE FROM verses WHERE date / (1000 * 60 * 60 * 24) = :date / 86400000 AND version = :version")
    suspend fun deleteByDayAndVersion(date: Date, version: String)

    @Delete
    suspend fun delete(verse: BibleVerse)
}