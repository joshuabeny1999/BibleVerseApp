package ch.joshuah.bibleverseapp.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.youversion.platform.core.api.YouVersionApi
import com.youversion.platform.core.bibles.api.BiblesApi
import com.youversion.platform.core.bibles.models.BibleVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class TranslationSelectionViewModel(
    private val biblesApi: BiblesApi = YouVersionApi.bible,
    private val sharedPreferences: SharedPreferences? = null,
    private val defaultLanguageCode: String = Locale.getDefault().language
) : ViewModel() {

    private val _allVersions = MutableStateFlow<List<BibleVersion>>(emptyList())

    private val _availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages: StateFlow<List<String>> = _availableLanguages.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String>(defaultLanguageCode)
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _filteredTranslations = MutableStateFlow<List<BibleVersion>>(emptyList())
    val filteredTranslations: StateFlow<List<BibleVersion>> = _filteredTranslations.asStateFlow()

    private val _selectedTranslationId = MutableStateFlow<String?>(null)
    val selectedTranslationId: StateFlow<String?> = _selectedTranslationId.asStateFlow()

    private val _isLoading = MutableStateFlow<Boolean>(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        val savedVersion = sharedPreferences?.getString(PREF_KEY_BIBLE_VERSION, null)
        if (!savedVersion.isNullOrBlank()) {
            _selectedTranslationId.value = savedVersion
        }
        loadVersions()
    }

    fun loadVersions() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = biblesApi.versions()
                val versions = response.data
                _allVersions.value = versions

                val languages = versions
                    .mapNotNull { it.languageTag?.lowercase()?.split("-")?.firstOrNull() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                _availableLanguages.value = languages

                if (languages.isNotEmpty()) {
                    val matchingLang = languages.find { it.equals(defaultLanguageCode, ignoreCase = true) }
                    val currentLang = matchingLang ?: _selectedLanguage.value.takeIf { languages.contains(it) } ?: languages.first()
                    _selectedLanguage.value = currentLang
                    updateFilteredTranslations(currentLang)
                }
            } catch (e: Exception) {
                // Ignore exception in loading versions
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectLanguage(languageCode: String) {
        _selectedLanguage.value = languageCode
        updateFilteredTranslations(languageCode)
    }

    private fun updateFilteredTranslations(languageCode: String) {
        val target = languageCode.lowercase()
        val filtered = _allVersions.value.filter { version ->
            val tag = version.languageTag?.lowercase().orEmpty()
            tag == target || tag.startsWith("$target-") || tag.split("-").firstOrNull() == target
        }
        _filteredTranslations.value = filtered
    }

    fun selectTranslation(version: BibleVersion) {
        val idStr = version.id.toString()
        val title = version.localizedTitle.orEmpty().ifBlank { version.title.orEmpty() }
        val abbrev = version.localizedAbbreviation.orEmpty().ifBlank { version.abbreviation.orEmpty() }
        val displayName = if (abbrev.isNotBlank() && !title.contains(abbrev)) "$title ($abbrev)" else title.ifBlank { idStr }

        _selectedTranslationId.value = idStr
        sharedPreferences?.edit()
            ?.putString(PREF_KEY_BIBLE_VERSION, idStr)
            ?.putString(PREF_KEY_BIBLE_VERSION_NAME, displayName)
            ?.apply()
    }

    fun selectTranslation(versionId: Int) {
        val foundVersion = _allVersions.value.find { it.id == versionId }
        if (foundVersion != null) {
            selectTranslation(foundVersion)
        } else {
            val idStr = versionId.toString()
            val versionName = foundVersion?.title ?: "Version $versionId"
            _selectedTranslationId.value = idStr
            sharedPreferences?.edit()
                ?.putString(PREF_KEY_BIBLE_VERSION, idStr)
                ?.putString(PREF_KEY_BIBLE_VERSION_NAME, versionName)
                ?.apply()
        }
    }

    companion object {
        const val PREF_KEY_BIBLE_VERSION = "bible_version"
        const val PREF_KEY_BIBLE_VERSION_NAME = "bible_version_name"
    }
}
