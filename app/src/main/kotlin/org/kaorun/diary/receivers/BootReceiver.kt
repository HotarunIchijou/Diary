package org.kaorun.diary.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.kaorun.diary.viewmodel.TasksViewModel

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            TasksViewModel.restoreNotifications(context)
        }
    }
}
