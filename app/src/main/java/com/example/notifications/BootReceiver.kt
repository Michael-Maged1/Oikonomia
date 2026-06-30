package com.example.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.toDomain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Boot completed! Restoring scheduled reminders.")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val activeTasks = db.taskDao().getAllTasksDirect().map { it.toDomain() }.filter { !it.isCompleted }
                    for (task in activeTasks) {
                        ReminderScheduler.scheduleReminder(context, task)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error restoring reminders on boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
