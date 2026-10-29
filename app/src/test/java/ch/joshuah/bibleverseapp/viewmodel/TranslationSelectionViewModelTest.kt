package ch.joshuah.bibleverseapp.viewmodel

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.youversion.platform.core.api.PaginatedResponse
import com.youversion.platform.core.bibles.api.BiblesApi
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TranslationSelectionViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()
    private val biblesApi: BiblesApi = mock()
    private val sharedPreferences: SharedPreferences = mock()
    private val sharedPreferencesEditor: SharedPreferences.Editor = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor)
        whenever(sharedPreferencesEditor.putString(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(sharedPreferencesEditor)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createVersion(id: Int, title: String, abbreviation: String, languageTag: String): BibleVersion {
        return BibleVersion(
            id = id,
            title = title,
            abbreviation = abbreviation,
            promotionalContent = "",
            copyright = "",
            languageTag = languageTag,
            localizedAbbreviation = abbreviation,
            localizedTitle = title,
            readerFooter = "",
            readerFooterUrl = "",
            bookCodes = emptyList(),
            books = emptyList(),
            textDirection = "ltr",
            organizationId = "1"
        )
    }

    @Test
    fun loadVersions_setsDefaultLanguageMatchingLocaleAndExtractsLanguages() = runTest {
        val germanVersion = createVersion(1, "Luther Bibel", "LUT", "de")
        val englishVersion = createVersion(2, "King James Version", "KJV", "en")
        val frenchVersion = createVersion(3, "Louis Segond", "LSG", "fr")

        val paginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(paginated.data).thenReturn(listOf(germanVersion, englishVersion, frenchVersion))
        whenever(biblesApi.versions()).thenReturn(paginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageCode = "de"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("de", viewModel.selectedLanguage.value)
        assertEquals(listOf("de", "en", "fr"), viewModel.availableLanguages.value)
        assertEquals(1, viewModel.filteredTranslations.value.size)
        assertEquals(germanVersion, viewModel.filteredTranslations.value[0])
    }

    @Test
    fun selectLanguage_filtersTranslationsBySelectedLanguage() = runTest {
        val germanVersion1 = createVersion(1, "Luther Bibel 1545", "LUTH1545", "de")
        val germanVersion2 = createVersion(2, "Schlachter 2000", "SCH2000", "de-DE")
        val englishVersion = createVersion(3, "New International Version", "NIV", "en")

        val paginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(paginated.data).thenReturn(listOf(germanVersion1, germanVersion2, englishVersion))
        whenever(biblesApi.versions()).thenReturn(paginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageCode = "de"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.filteredTranslations.value.size)

        viewModel.selectLanguage("en")

        assertEquals("en", viewModel.selectedLanguage.value)
        assertEquals(1, viewModel.filteredTranslations.value.size)
        assertEquals(englishVersion, viewModel.filteredTranslations.value[0])
    }

    @Test
    fun selectTranslation_savesTranslationIdToSharedPreferences() = runTest {
        val paginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(paginated.data).thenReturn(emptyList())
        whenever(biblesApi.versions()).thenReturn(paginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageCode = "en"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTranslation(111)

        assertEquals("111", viewModel.selectedTranslationId.value)
        verify(sharedPreferencesEditor).putString(TranslationSelectionViewModel.PREF_KEY_BIBLE_VERSION, "111")
        verify(sharedPreferencesEditor).apply()
    }

    @Test
    fun loadVersions_whenDefaultLanguageNotFound_fallsBackToFirstLanguage() = runTest {
        val germanVersion = createVersion(1, "Luther Bibel", "LUT", "de")
        val englishVersion = createVersion(2, "King James Version", "KJV", "en")

        val paginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(paginated.data).thenReturn(listOf(germanVersion, englishVersion))
        whenever(biblesApi.versions()).thenReturn(paginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageCode = "es" // Spanish not present
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("de", viewModel.selectedLanguage.value)
        assertEquals(1, viewModel.filteredTranslations.value.size)
        assertEquals(germanVersion, viewModel.filteredTranslations.value[0])
    }

    @Test
    fun init_loadsSavedTranslationIdFromSharedPreferences() = runTest {
        whenever(sharedPreferences.getString(TranslationSelectionViewModel.PREF_KEY_BIBLE_VERSION, null)).thenReturn("314")
        val paginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(paginated.data).thenReturn(emptyList())
        whenever(biblesApi.versions()).thenReturn(paginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageCode = "en"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("314", viewModel.selectedTranslationId.value)
    }
}
