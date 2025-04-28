package org.kaorun.diary.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.kaorun.diary.viewmodel.TasksViewModel

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("BootReceiver", "BootReceiver triggered")
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed, restoring notifications...")
            TasksViewModel.restoreNotifications(context)
        }
    }
}
