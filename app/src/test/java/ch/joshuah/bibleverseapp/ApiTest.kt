package ch.joshuah.bibleverseapp

import ch.joshuah.bibleverseapp.services.BibleVerseApiService
import org.junit.Assert.assertNotNull
import org.junit.Test

class ApiTest {
    @Test
    fun testApiServiceInstantiation() {
        val service = BibleVerseApiService()
        assertNotNull(service)
    }
}
