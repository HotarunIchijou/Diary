package org.kaorun.diary.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import org.kaorun.diary.data.SettingsItem
import org.kaorun.diary.databinding.ActivityAboutBinding
import org.kaorun.diary.ui.adapters.AboutAdapter
import androidx.core.net.toUri
import org.kaorun.diary.R

class AboutActivity : BaseActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        binding.version.text = versionName

        val settingsItems = listOf(
            SettingsItem(
                title = getString(R.string.source_code),
                summary = getString(R.string.source_code_summary),
                url = "https://github.com/HotarunIchijou/diary",
                icon = R.drawable.code_24px,
                targetActivity = null),

            SettingsItem(
                title = getString(R.string.contact_developer),
                summary = getString(R.string.contact_developer_summary),
                url = "https://t.me/KaorunIchijou",
                icon = R.drawable.support_agent_24px,
                targetActivity = null,
            )
        )

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = AboutAdapter(
            settingsItems,
            onUrlClick = { url ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = url.toUri()
                }
                startActivity(intent)
            }
        )

        binding.topAppBar.setNavigationOnClickListener { finish() }
    }
}
