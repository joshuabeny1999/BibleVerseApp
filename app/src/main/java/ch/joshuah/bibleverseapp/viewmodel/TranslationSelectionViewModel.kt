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
    defaultLanguageInput: String = Locale.getDefault().language
) : ViewModel() {

    private val defaultLanguageCode: String = mapToYouVersionLanguageCode(defaultLanguageInput)

    private val _allVersions = MutableStateFlow<List<BibleVersion>>(emptyList())

    private val _availableLanguages = MutableStateFlow<List<String>>(
        listOf("eng", "deu", "spa", "fra", "por", "ita", "nld", "rus", "zho", "kor", "jpn", "ara")
    )
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

        val initialLanguage = if (_availableLanguages.value.contains(defaultLanguageCode)) {
            defaultLanguageCode
        } else {
            _availableLanguages.value.firstOrNull() ?: "eng"
        }
        _selectedLanguage.value = initialLanguage
        loadVersionsForLanguage(initialLanguage)
    }

    fun loadVersions() {
        loadVersionsForLanguage(_selectedLanguage.value)
    }

    fun selectLanguage(languageCode: String) {
        val mappedCode = mapToYouVersionLanguageCode(languageCode)
        _selectedLanguage.value = mappedCode
        loadVersionsForLanguage(mappedCode)
    }

    private fun loadVersionsForLanguage(languageCode: String) {
        viewModelScope.launch {
            val memoryCached = cachedVersionsMap[languageCode]
            if (!memoryCached.isNullOrEmpty()) {
                _allVersions.value = memoryCached
                _filteredTranslations.value = memoryCached
                return@launch
            }

            _isLoading.value = true
            try {
                var response = biblesApi.versions(languageCode = languageCode, pageSize = 50)
                var versions = response.data

                if (versions.isEmpty()) {
                    val altCode = when (languageCode) {
                        "eng" -> "en"
                        "deu" -> "de"
                        "spa" -> "es"
                        "fra" -> "fr"
                        "por" -> "pt"
                        "ita" -> "it"
                        "nld" -> "nl"
                        "rus" -> "ru"
                        "zho" -> "zh"
                        "kor" -> "ko"
                        "jpn" -> "ja"
                        "ara" -> "ar"
                        else -> ""
                    }
                    if (altCode.isNotBlank()) {
                        response = biblesApi.versions(languageCode = altCode, pageSize = 50)
                        versions = response.data
                    }
                }

                if (versions.isEmpty()) {
                    val allResponse = biblesApi.versions(languageCode = null, pageSize = 99)
                    val allVers = allResponse.data
                    if (allVers.isNotEmpty()) {
                        val filtered = filterVersionsByLanguage(allVers, languageCode)
                        versions = filtered.ifEmpty { allVers }
                    }
                }

                if (versions.isNotEmpty()) {
                    cachedVersionsMap[languageCode] = versions
                    _allVersions.value = versions
                    _filteredTranslations.value = versions

                    val currentLangs = _availableLanguages.value.toMutableList()
                    if (!currentLangs.contains(languageCode)) {
                        currentLangs.add(0, languageCode)
                        _availableLanguages.value = currentLangs
                    }
                } else {
                    _filteredTranslations.value = emptyList()
                }
            } catch (e: Exception) {
                try {
                    val allResponse = biblesApi.versions(languageCode = null, pageSize = 99)
                    val allVers = allResponse.data
                    if (allVers.isNotEmpty()) {
                        _allVersions.value = allVers
                        val filtered = filterVersionsByLanguage(allVers, languageCode)
                        val finalVers = filtered.ifEmpty { allVers }
                        cachedVersionsMap[languageCode] = finalVers
                        _filteredTranslations.value = finalVers
                    }
                } catch (_: Exception) {
                    // Ignore exception if rate limited, UI keeps previous state gracefully
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun filterVersionsByLanguage(versions: List<BibleVersion>, languageCode: String): List<BibleVersion> {
        val targetLang = languageCode.lowercase()
        return versions.filter { ver ->
            val tag = ver.languageTag.orEmpty().lowercase()
            tag.startsWith(targetLang) ||
                (targetLang == "eng" && (tag.startsWith("en") || tag.startsWith("eng"))) ||
                (targetLang == "deu" && (tag.startsWith("de") || tag.startsWith("deu"))) ||
                (targetLang == "spa" && (tag.startsWith("es") || tag.startsWith("spa"))) ||
                (targetLang == "fra" && (tag.startsWith("fr") || tag.startsWith("fra"))) ||
                (targetLang == "por" && (tag.startsWith("pt") || tag.startsWith("por"))) ||
                (targetLang == "ita" && (tag.startsWith("it") || tag.startsWith("ita"))) ||
                (targetLang == "nld" && (tag.startsWith("nl") || tag.startsWith("nld"))) ||
                (targetLang == "rus" && (tag.startsWith("ru") || tag.startsWith("rus"))) ||
                (targetLang == "zho" && (tag.startsWith("zh") || tag.startsWith("zho"))) ||
                (targetLang == "kor" && (tag.startsWith("ko") || tag.startsWith("kor"))) ||
                (targetLang == "jpn" && (tag.startsWith("ja") || tag.startsWith("jpn"))) ||
                (targetLang == "ara" && (tag.startsWith("ar") || tag.startsWith("ara")))
        }
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
            val versionName = "Version $versionId"
            _selectedTranslationId.value = idStr
            sharedPreferences?.edit()
                ?.putString(PREF_KEY_BIBLE_VERSION, idStr)
                ?.putString(PREF_KEY_BIBLE_VERSION_NAME, versionName)
                ?.apply()
        }
    }

    private fun mapToYouVersionLanguageCode(inputCode: String): String {
        val code = inputCode.lowercase().trim()
        return when (code) {
            "en", "eng", "en-us", "en-gb" -> "eng"
            "de", "deu", "de-de", "ger" -> "deu"
            "es", "spa", "es-es" -> "spa"
            "fr", "fra", "fr-fr", "fre" -> "fra"
            "pt", "por", "pt-br", "pt-pt" -> "por"
            "it", "ita" -> "ita"
            "nl", "nld", "dut" -> "nld"
            "ru", "rus" -> "rus"
            "zh", "zho", "chi" -> "zho"
            "ko", "kor" -> "kor"
            "ja", "jpn" -> "jpn"
            "ar", "ara" -> "ara"
            else -> if (code.length == 3) code else "eng"
        }
    }

    companion object {
        const val PREF_KEY_BIBLE_VERSION = "bible_version"
        const val PREF_KEY_BIBLE_VERSION_NAME = "bible_version_name"

        private val cachedVersionsMap = mutableMapOf<String, List<BibleVersion>>()
    }
}
