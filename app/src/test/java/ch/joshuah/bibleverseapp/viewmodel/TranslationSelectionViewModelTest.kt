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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
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
    fun loadVersions_setsDefaultLanguageMatchingLocaleAndLoadsTranslations() = runTest {
        val germanVersion = createVersion(1, "Luther Bibel", "LUT", "deu")

        val paginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(paginated.data).thenReturn(listOf(germanVersion))
        whenever(biblesApi.versions(eq("deu"), anyOrNull(), eq(50), anyOrNull())).thenReturn(paginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageInput = "de"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("deu", viewModel.selectedLanguage.value)
        assertEquals(1, viewModel.filteredTranslations.value.size)
        assertEquals(germanVersion, viewModel.filteredTranslations.value[0])
    }

    @Test
    fun selectLanguage_filtersTranslationsBySelectedLanguage() = runTest {
        val germanVersion = createVersion(1, "Luther Bibel", "LUT", "deu")
        val englishVersion = createVersion(2, "New International Version", "NIV", "eng")

        val germanPaginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(germanPaginated.data).thenReturn(listOf(germanVersion))
        whenever(biblesApi.versions(eq("deu"), anyOrNull(), eq(50), anyOrNull())).thenReturn(germanPaginated)

        val englishPaginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(englishPaginated.data).thenReturn(listOf(englishVersion))
        whenever(biblesApi.versions(eq("eng"), anyOrNull(), eq(50), anyOrNull())).thenReturn(englishPaginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageInput = "de"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("deu", viewModel.selectedLanguage.value)
        assertEquals(1, viewModel.filteredTranslations.value.size)

        viewModel.selectLanguage("eng")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("eng", viewModel.selectedLanguage.value)
        assertEquals(1, viewModel.filteredTranslations.value.size)
        assertEquals(englishVersion, viewModel.filteredTranslations.value[0])
    }

    @Test
    fun selectTranslation_savesTranslationIdToSharedPreferences() = runTest {
        val paginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(paginated.data).thenReturn(emptyList())
        whenever(biblesApi.versions(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(paginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageInput = "eng"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectTranslation(111)

        assertEquals("111", viewModel.selectedTranslationId.value)
        verify(sharedPreferencesEditor).putString(TranslationSelectionViewModel.PREF_KEY_BIBLE_VERSION, "111")
        verify(sharedPreferencesEditor).putString(TranslationSelectionViewModel.PREF_KEY_BIBLE_VERSION_NAME, "Version 111")
        verify(sharedPreferencesEditor).apply()
    }

    @Test
    fun init_loadsSavedTranslationIdFromSharedPreferences() = runTest {
        whenever(sharedPreferences.getString(TranslationSelectionViewModel.PREF_KEY_BIBLE_VERSION, null)).thenReturn("314")
        val paginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(paginated.data).thenReturn(emptyList())
        whenever(biblesApi.versions(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(paginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageInput = "eng"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("314", viewModel.selectedTranslationId.value)
    }

    @Test
    fun loadVersions_fallsBackToFetchingAllVersionsWhenLanguageSpecificReturnsEmpty() = runTest {
        val emptyPaginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(emptyPaginated.data).thenReturn(emptyList())
        whenever(biblesApi.versions(eq("deu"), anyOrNull(), eq(50), anyOrNull())).thenReturn(emptyPaginated)
        whenever(biblesApi.versions(eq("de"), anyOrNull(), eq(50), anyOrNull())).thenReturn(emptyPaginated)

        val germanVersion = createVersion(1, "Luther Bibel", "LUT", "de-DE")
        val allPaginated = mock<PaginatedResponse<BibleVersion>>()
        whenever(allPaginated.data).thenReturn(listOf(germanVersion))
        whenever(biblesApi.versions(eq(null), anyOrNull(), eq(99), anyOrNull())).thenReturn(allPaginated)

        val viewModel = TranslationSelectionViewModel(
            biblesApi = biblesApi,
            sharedPreferences = sharedPreferences,
            defaultLanguageInput = "de"
        )

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("deu", viewModel.selectedLanguage.value)
        assertEquals(1, viewModel.filteredTranslations.value.size)
        assertEquals(germanVersion, viewModel.filteredTranslations.value[0])
    }
}
