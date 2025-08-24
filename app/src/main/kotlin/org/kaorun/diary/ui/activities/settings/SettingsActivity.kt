package org.kaorun.diary.ui.activities.settings

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import org.kaorun.diary.R
import org.kaorun.diary.data.SettingsItem
import org.kaorun.diary.databinding.ActivitySettingsBinding
import org.kaorun.diary.ui.activities.BaseActivity
import org.kaorun.diary.ui.adapters.SettingsAdapter
import org.kaorun.diary.utils.InsetsHandler
import org.kaorun.diary.utils.VerticalSpaceItemDecoration

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupInsets()
        setupToolbar()
        setupRecyclerView()
    }

    private fun setupInsets() {
        InsetsHandler.applyViewInsets(binding.recyclerView)
        InsetsHandler.applyAppBarInsets(binding.appBarLayout)
    }

    private fun setupToolbar() {
        binding.appBarLayout.setExpanded(false)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val settingsItems = listOfNotNull(
            SettingsItem(
                title = getString(R.string.appearance),
                summary = getString(R.string.appearance_summary),
                icon = R.drawable.palette_24px,
                targetActivity = AppearanceActivity::class.java
            ),

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            SettingsItem(
                title = getString(R.string.language),
                summary = getString(R.string.language_summary),
                icon = R.drawable.language_24px,
                targetActivity = null,
                url = null,
                specialAction = Settings.ACTION_APP_LOCALE_SETTINGS
            ) else null,

            SettingsItem(
                title = getString(R.string.about),
                summary = getString(R.string.about_summary),
                icon = R.drawable.info_24px,
                targetActivity = AboutActivity::class.java
            )
        )

        val recyclerView = binding.recyclerView
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(this))
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SettingsAdapter(settingsItems) { item ->
            when {
                item.targetActivity != null -> {
                    startActivity(Intent(this, item.targetActivity))
                }
                item.url != null -> {
                    val intent = Intent(Intent.ACTION_VIEW, item.url.toUri())
                    startActivity(intent)
                }
                item.specialAction != null -> {
                    val intent = Intent(item.specialAction).apply {
                        data = "package:$packageName".toUri()
                    }
                    startActivity(intent)
                }
            }
        }
    }
}