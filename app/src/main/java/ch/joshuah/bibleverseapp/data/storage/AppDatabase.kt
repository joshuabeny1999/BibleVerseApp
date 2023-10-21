package ch.joshuah.bibleverseapp.data.storage

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.joshuah.bibleverseapp.data.BibleVerse

@Database(entities = [BibleVerse::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bibleVerseDao(): BibleVerseDao
}
