package org.kaorun.diary.ui.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import org.kaorun.diary.R

abstract class BaseActivity : AppCompatActivity() {

    protected var cachedThemeIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        cachedThemeIndex = prefs.getInt("color_scheme", 0)
        setTheme(getThemeResFromIndex(cachedThemeIndex))
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val currentIndex = prefs.getInt("color_scheme", 0)

        if (currentIndex != cachedThemeIndex) {
            recreate()
        }
    }

    private fun getThemeResFromIndex(index: Int): Int {
        return when (index) {
            1 -> R.style.Green
            2 -> R.style.Red
            3 -> R.style.Blue
            4 -> R.style.Yellow
            else -> R.style.Base_Theme_Diary
        }
    }
}
