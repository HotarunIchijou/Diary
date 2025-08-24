package org.kaorun.diary.ui.activities.settings

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import org.kaorun.diary.R
import org.kaorun.diary.data.SettingsItem
import org.kaorun.diary.databinding.ActivityAboutBinding
import org.kaorun.diary.ui.activities.BaseActivity
import org.kaorun.diary.ui.adapters.AboutAdapter
import org.kaorun.diary.utils.InsetsHandler
import org.kaorun.diary.utils.VerticalSpaceItemDecoration

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        binding.version.text = versionName

        setupInsets()
        setupToolbar()
        setupRecyclerView()
    }

    private fun setupInsets() {
        InsetsHandler.applyViewInsets(binding.recyclerView)
        InsetsHandler.applyAppBarInsets(binding.appBarLayout)
    }

    private fun setupToolbar() {
        binding.topAppBar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        val settingsItems = listOf(
            SettingsItem(
                title = getString(R.string.source_code),
                summary = getString(R.string.source_code_summary),
                url = "https://github.com/HotarunIchijou/diary",
                icon = R.drawable.code_24px,
                targetActivity = null
            ),

            SettingsItem(
                title = getString(R.string.license),
                summary = getString(R.string.license_summary),
                url = "https://github.com/HotarunIchijou/Diary/blob/master/LICENSE",
                icon = R.drawable.balance_24px,
                targetActivity = null,
            ),

            SettingsItem(
                title = getString(R.string.contact_developer),
                summary = getString(R.string.contact_developer_summary),
                url = "https://t.me/KaorunIchijou",
                icon = R.drawable.support_agent_24px,
                targetActivity = null,
            )
        )

        val recyclerView = binding.recyclerView
        recyclerView.addItemDecoration(VerticalSpaceItemDecoration(this))
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
    }
}