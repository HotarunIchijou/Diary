package org.kaorun.diary.receivers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MarkAsCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            return
        }

        val database = FirebaseDatabase.getInstance()
            .getReference("Tasks")
            .child(user.uid)
            .child(taskId)

        database.child("c").setValue(true)
            .addOnSuccessListener {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(taskId.hashCode())
            }
    }
}
