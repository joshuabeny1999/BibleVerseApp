package ch.joshuah.bibleverseapp

import ch.joshuah.bibleverseapp.services.BibleVerseApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class ApiTest {
    @Test
    fun testApi() = runBlocking {
        val service = BibleVerseApiService()
        val result = service.fetchBibleVerse("NIV").first()
        println("Result: $result")
    }
}
