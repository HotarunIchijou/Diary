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
import org.kaorun.diary.ui.adapters.ThemeAdapter
import org.kaorun.diary.ui.fragments.AppearanceFragment
import org.kaorun.diary.utils.InsetsHandler

class AppearanceActivity : BaseSettingsActivity() {
    private lateinit var binding: ActivityAppearanceBinding
    private val prefs by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppearanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupToolbar(binding.appBarLayout, binding.toolbar, binding.collapsingToolbar)
        setupRecyclerView()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AppearanceFragment())
                .commit()
        }
    }

    private fun setupInsets() {
        InsetsHandler.applyViewInsets(binding.recyclerView)
        InsetsHandler.applyViewInsets(binding.illustration)
        InsetsHandler.applyAppBarInsets(binding.appBarLayout)
    }

    private fun setupRecyclerView() {
        val systemCtx = ContextThemeWrapper(this, R.style.Base_Theme_Diary)

        val themes = listOf(
            ThemePreview(
                systemCtx.getThemeColor(com.google.android.material.R.attr.colorPrimaryVariant),
                systemCtx.getThemeColor(com.google.android.material.R.attr.colorSecondary),
                systemCtx.getThemeColor(com.google.android.material.R.attr.colorTertiary)
            ),

            ThemePreview(
                ContextCompat.getColor(this, R.color.md_theme_primary),
                ContextCompat.getColor(this, R.color.md_theme_secondary),
                ContextCompat.getColor(this, R.color.md_theme_tertiary)
            ),

            ThemePreview(
                ContextCompat.getColor(this, R.color.red_theme_primary),
                ContextCompat.getColor(this, R.color.red_theme_secondary),
                ContextCompat.getColor(this, R.color.red_theme_tertiary)
            ),

            ThemePreview(
                ContextCompat.getColor(this, R.color.blue_theme_primary),
                ContextCompat.getColor(this, R.color.blue_theme_secondary),
                ContextCompat.getColor(this, R.color.blue_theme_tertiary)
            ),

            ThemePreview(
                ContextCompat.getColor(this, R.color.yellow_theme_primary),
                ContextCompat.getColor(this, R.color.yellow_theme_secondary),
                ContextCompat.getColor(this, R.color.yellow_theme_tertiary)
            ),
        )

        val adapter = ThemeAdapter(themes, prefs) { _ -> recreate() }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(
                this@AppearanceActivity,
                LinearLayoutManager.HORIZONTAL, false
            )
            this.adapter = adapter
        }
    }

    private fun Context.getThemeColor(attrResId: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrResId, typedValue, true)
        return typedValue.data
    }
}