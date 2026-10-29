package ch.joshuah.bibleverseapp.fragments

import android.Manifest
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import ch.joshuah.bibleverseapp.R
import ch.joshuah.bibleverseapp.services.DailyVerseService
import ch.joshuah.bibleverseapp.widgets.WidgetUpdateScheduler
import com.jaredrummler.android.colorpicker.ColorPreferenceCompat

class SettingFragment : PreferenceFragmentCompat() {
    private lateinit var notificationEnabledPref: SwitchPreferenceCompat
    private lateinit var bibleVersionPreference: Preference
    private lateinit var widgetColorPreference: ColorPreferenceCompat
    private lateinit var widgetFontSizePreference: ListPreference
    private lateinit var sharedPrefs: SharedPreferences

    private var dailyVerseService = DailyVerseService()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                dailyVerseService.setNotificationAlarm(requireContext(), sharedPrefs)
            } else {
                notificationEnabledPref.isChecked = false
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        notificationEnabledPref = findPreference("enable_notifications")!!
        widgetColorPreference = findPreference("widget_color")!!
        bibleVersionPreference = findPreference("bible_version")!!
        widgetFontSizePreference = findPreference("widget_font_size")!!

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        notificationEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            val enabled = newValue as Boolean
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    requestNotificationPermission()
                } else {
                    dailyVerseService.setNotificationAlarm(requireContext(), sharedPrefs)
                }
            } else {
                dailyVerseService.cancelNotificationAlarm(requireContext())
            }
            true
        }

        widgetColorPreference.setOnPreferenceChangeListener { _, _ ->
            WidgetUpdateScheduler.updateAllWidgets(requireContext())
            true
        }

        bibleVersionPreference.setOnPreferenceClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.activity_main_container, TranslationSelectionFragment())
                .addToBackStack(null)
                .commit()
            true
        }

        widgetFontSizePreference.setOnPreferenceChangeListener { _, _ ->
            WidgetUpdateScheduler.updateAllWidgets(requireContext())
            true
        }
    }

    override fun onResume() {
        super.onResume()
        updateBibleVersionSummary()
    }

    private fun updateBibleVersionSummary() {
        val savedName = sharedPrefs.getString("bible_version_name", null)
        val savedVersion = sharedPrefs.getString("bible_version", null)
        bibleVersionPreference.summary = savedName ?: savedVersion ?: getString(R.string.preference_listPreference_bible_version_summary)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        requestPermissionLauncher.launch(permission)
    }
}
