package ch.joshuah.bibleverseapp.data

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "verses")
data class BibleVerse(
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "reference") val reference: String,
    @ColumnInfo(name = "version") val version: String,
    @ColumnInfo(name = "version_long") val versionLong: String,
    @ColumnInfo(name = "link") val link: Uri,
    @ColumnInfo(name = "date") val date: Date,
    @PrimaryKey(autoGenerate = true) val uid: Int = 0
    )
