package com.example.notifications

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.domain.model.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleReminder(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        val triggerTimeMs = calculateTriggerTime(task.dueDate, task.dueTime)
        if (triggerTimeMs <= System.currentTimeMillis()) {
            Log.d(TAG, "Trigger time is in the past, skipping alarm for task: ${task.title}")
            return
        }

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
            putExtra("TASK_DESC", task.description)
        }

        // Generate unique pending intent ID based on task ID hash
        val requestCode = task.id.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder for task '${task.title}' at triggerTimeMs: $triggerTimeMs")
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to schedule exact alarm due to permission constraints", e)
            // Fallback to non-exact alarm
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTimeMs,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, taskId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = taskId.hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Canceled reminder for task: $taskId")
        }
    }

    private fun calculateTriggerTime(dueDateStr: String, dueTimeStr: String): Long {
        val calendar = Calendar.getInstance()
        try {
            val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = dateSdf.parse(dueDateStr) ?: return System.currentTimeMillis()
            val dateCalendar = Calendar.getInstance().apply { time = date }

            val timeParts = dueTimeStr.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 12
            val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

            calendar.apply {
                set(Calendar.YEAR, dateCalendar.get(Calendar.YEAR))
                set(Calendar.MONTH, dateCalendar.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, dateCalendar.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing reminder date/time", e)
        }
        return calendar.timeInMillis
    }
}
