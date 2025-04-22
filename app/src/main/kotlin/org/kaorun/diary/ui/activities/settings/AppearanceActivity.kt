package org.kaorun.diary.ui.activities.settings

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import org.kaorun.diary.R
import org.kaorun.diary.data.ThemePreview
import org.kaorun.diary.databinding.ActivityAppearanceBinding
import org.kaorun.diary.ui.activities.BaseActivity
import org.kaorun.diary.ui.adapters.ThemeAdapter
import org.kaorun.diary.ui.fragments.AppearanceFragment

class AppearanceActivity : BaseActivity() {
    private lateinit var binding: ActivityAppearanceBinding
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppearanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }



        val systemCtx = ContextThemeWrapper(this, R.style.Base_Theme_Diary)

        val themes = listOf(
            ThemePreview(
                systemCtx.getThemeColor(com.google.android.material.R.attr.colorPrimaryFixedDim),
                systemCtx.getThemeColor(com.google.android.material.R.attr.colorOnSecondaryFixedVariant),
                systemCtx.getThemeColor(com.google.android.material.R.attr.colorOnTertiaryFixedVariant)
            ),

            ThemePreview(
                ContextCompat.getColor(this, R.color.md_theme_primaryFixedDim),
                ContextCompat.getColor(this, R.color.md_theme_onSecondaryFixedVariant),
                ContextCompat.getColor(this, R.color.md_theme_onTertiaryFixedVariant)
            ),

            ThemePreview(
                ContextCompat.getColor(this, R.color.red_theme_primaryFixedDim),
                ContextCompat.getColor(this, R.color.red_theme_onSecondaryFixedVariant),
                ContextCompat.getColor(this, R.color.red_theme_onTertiaryFixedVariant)
            ),

            ThemePreview(
                ContextCompat.getColor(this, R.color.blue_theme_primaryFixedDim),
                ContextCompat.getColor(this, R.color.blue_theme_onSecondaryFixedVariant),
                ContextCompat.getColor(this, R.color.blue_theme_onTertiaryFixedVariant)
            ),

            ThemePreview(
                ContextCompat.getColor(this, R.color.yellow_theme_primaryFixedDim),
                ContextCompat.getColor(this, R.color.yellow_theme_onSecondaryFixedVariant),
                ContextCompat.getColor(this, R.color.yellow_theme_onTertiaryFixedVariant)
            ),
        )


        val adapter = ThemeAdapter(themes, prefs) { newIndex -> recreate() }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@AppearanceActivity,
                LinearLayoutManager.HORIZONTAL, false
            )
            this.adapter = adapter
        }


        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AppearanceFragment())
                .commit()
        }
    }

    fun Context.getThemeColor(attrResId: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrResId, typedValue, true)
        return typedValue.data
    }
}