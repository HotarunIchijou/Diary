package org.kaorun.diary.ui.activities

import androidx.lifecycle.ViewModelProvider
import org.kaorun.diary.ui.fragments.BottomSheetFragment
import org.kaorun.diary.viewmodel.TasksViewModel


class TaskAddActivity: BaseActivity() {
    override fun onResume() {
        super.onResume()

        val tasksViewModel: TasksViewModel = ViewModelProvider(this)[TasksViewModel::class.java]
        tasksViewModel.getBottomSheetDismissed().observe(this) { value ->
            if (value)
                finish()
        }

        val bottomSheet = BottomSheetFragment()
        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
    }
}