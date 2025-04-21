package org.kaorun.diary.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.recyclerview.widget.LinearLayoutManager
import org.kaorun.diary.R
import org.kaorun.diary.data.SettingsItem
import org.kaorun.diary.databinding.ActivitySettingsBinding
import org.kaorun.diary.ui.adapters.SettingsAdapter

class SettingsActivity : BaseActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val settingsItems = listOf(
            SettingsItem(getString(R.string.appearance),
                getString(R.string.appearance_summary),
                R.drawable.palette_24px,
                AppearanceActivity::class.java),

            SettingsItem(getString(R.string.about),
                getString(R.string.about_summary),
                R.drawable.info_24px,
                AboutActivity::class.java)
        )

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SettingsAdapter(settingsItems) { activityClass ->
            startActivity(Intent(this, activityClass))
        }

        binding.toolbar.setNavigationOnClickListener { finish() }
    }
}