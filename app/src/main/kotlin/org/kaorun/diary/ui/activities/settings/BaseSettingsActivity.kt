package org.kaorun.diary.ui.activities.settings

import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.appbar.MaterialToolbar
import org.kaorun.diary.R
import org.kaorun.diary.ui.activities.BaseActivity

abstract class BaseSettingsActivity : BaseActivity() {
    protected fun setupToolbar(
        appBarLayout: AppBarLayout,
        toolbar: MaterialToolbar,
        collapsingToolbar: CollapsingToolbarLayout
    ) {
        appBarLayout.setExpanded(false)
        toolbar.setNavigationOnClickListener { finish() }
        collapsingToolbar.apply {
            val font = ResourcesCompat.getFont(context, R.font.google_sans_flex_round)
            val typeface = Typeface.create(font, 500, false)
            setCollapsedTitleTypeface(typeface)
            setExpandedTitleTypeface(typeface)
        }
    }
}