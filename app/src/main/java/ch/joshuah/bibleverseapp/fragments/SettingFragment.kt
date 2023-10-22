package ch.joshuah.bibleverseapp.fragments

import android.Manifest
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat
import ch.joshuah.bibleverseapp.R
import ch.joshuah.bibleverseapp.preference.TimePickerPreference
import ch.joshuah.bibleverseapp.services.DailyVerseService


class SettingFragment : PreferenceFragmentCompat() {
    private lateinit var notificationEnabledPref: SwitchPreferenceCompat
    private lateinit var timePickerPreference: TimePickerPreference
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
        timePickerPreference = findPreference("notifications_time")!!



        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        notificationEnabledPref.setOnPreferenceChangeListener { _, newValue ->
            println("Notification enabled: $newValue")
            val enabled = newValue as Boolean
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                    requestNotificationPermission()
                } else {
                    dailyVerseService.setNotificationAlarm(requireContext(), sharedPrefs)
                }
            } else {
                dailyVerseService.cancelNotificationAlarm(requireContext())
            }
            true
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestNotificationPermission() {
        val permission = Manifest.permission.POST_NOTIFICATIONS
        requestPermissionLauncher.launch(permission)
    }


}
