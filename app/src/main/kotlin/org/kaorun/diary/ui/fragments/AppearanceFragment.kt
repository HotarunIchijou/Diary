package org.kaorun.diary.ui.fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.kaorun.diary.R
import androidx.core.content.edit

class AppearanceFragment : PreferenceFragmentCompat() {

    private lateinit var themeOptions: Array<String>

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings_appearance, rootKey)

        themeOptions = resources.getStringArray(R.array.theme_options)

        val themePref = findPreference<Preference>("theme_mode")
        themePref?.summary = getThemeSummary()

        themePref?.setOnPreferenceClickListener {
            showThemeDialog()
            true
        }
    }

    private fun showThemeDialog() {
        val selected = getSelectedThemeIndex()

        val dialog = MaterialAlertDialogBuilder(requireContext(),
            com.google.android.material.R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered)
            .setIcon(getDrawable(requireContext(), R.drawable.brightness_6_24px))
            .setTitle(getString(R.string.app_theme))
            .setSingleChoiceItems(themeOptions, selected) { dialogInterface, which ->
                val mode = when (which) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                AppCompatDelegate.setDefaultNightMode(mode)
                saveThemePreference(which)
                dialogInterface.dismiss()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()

        dialog.show()
    }

    private fun saveThemePreference(index: Int) {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
            .edit { putInt("theme_mode", index) }

        findPreference<Preference>("theme_mode")?.summary = getThemeSummary()
    }

    private fun getSelectedThemeIndex(): Int {
        return PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getInt("theme_mode", 0)
    }

    private fun getThemeSummary(): String {
        return themeOptions[getSelectedThemeIndex()]
    }
}
