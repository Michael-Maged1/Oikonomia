package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.MainActivity
import com.example.data.local.AppDatabase
import com.example.data.local.toDomain
import com.example.data.local.PreferenceHelper
import com.example.data.repository.TaskRepository
import com.example.domain.model.TaskPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    private val NOTIFICATION_ID_BASE = 1000

    companion object {
        private const val TAG = "ReminderReceiver"
        private const val CHANNEL_ID_HIGH = "oikonomia_reminders_high"
        private const val CHANNEL_ID_NORMAL = "oikonomia_reminders_normal"
        private const val CHANNEL_ID_LOW = "oikonomia_reminders_low"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val preferenceHelper = PreferenceHelper(context)
        val language = preferenceHelper.language

        val action = intent.action
        val taskId = intent.getStringExtra("TASK_ID") ?: return
        var taskTitle = intent.getStringExtra("TASK_TITLE") ?: ""
        if (taskTitle.isEmpty() || taskTitle == "SopApp Reminder") {
            taskTitle = if (language == "ar") "أويكونوميا" else "أويكونوميا"
        }
        val taskDesc = intent.getStringExtra("TASK_DESC") ?: ""

        val db = AppDatabase.getDatabase(context)
        val repository = TaskRepository(db.taskDao())

        // 1. Handle complete action immediately
        if (action == "ACTION_COMPLETE") {
            Log.d(TAG, "Notification ACTION_COMPLETE triggered for task $taskId")
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val entity = db.taskDao().getTaskById(taskId)
                    if (entity != null) {
                        val domainTask = entity.toDomain()
                        val completedTask = domainTask.copy(isCompleted = true)
                        repository.saveTask(completedTask)
                    }
                    dismissNotification(context, taskId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error marking task complete from notification", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // 2. Handle snooze action (using selected snooze duration)
        if (action == "ACTION_REMIND_LATER" || action == "ACTION_SNOOZE") {
            Log.d(TAG, "Notification SNOOZE triggered for task $taskId")
            val snoozeMins = preferenceHelper.snoozeDuration
            val calendar = Calendar.getInstance().apply {
                add(Calendar.MINUTE, snoozeMins)
            }
            
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val entity = db.taskDao().getTaskById(taskId)
                    if (entity != null) {
                        val domainTask = entity.toDomain()
                        val hr = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY))
                        val min = String.format("%02d", calendar.get(Calendar.MINUTE))
                        val yr = calendar.get(Calendar.YEAR)
                        val mo = String.format("%02d", calendar.get(Calendar.MONTH) + 1)
                        val dy = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH))
                        
                        val postponedTask = domainTask.copy(
                            dueDate = "$yr-$mo-$dy",
                            dueTime = "$hr:$min"
                        )
                        repository.saveTask(postponedTask)
                        ReminderScheduler.scheduleReminder(context, postponedTask)
                    }
                    dismissNotification(context, taskId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error postponing task", e)
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // 3. Wake screen
        wakeUpScreen(context)

        // 4. Fetch the task asynchronously to get priority, then show notification with correct vibration pattern
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entity = db.taskDao().getTaskById(taskId)
                val task = entity?.toDomain()
                val priority = task?.priority ?: TaskPriority.NORMAL
                
                showNotification(context, taskId, taskTitle, taskDesc, language, priority, preferenceHelper)
            } catch (e: Exception) {
                Log.e(TAG, "Error showing notification", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun wakeUpScreen(context: Context) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "oikonomia:ReminderWakeLock"
                )
                wakeLock.acquire(4000) // Keep screen on for 4 seconds
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to wake up screen", e)
        }
    }

    private fun showNotification(
        context: Context,
        taskId: String,
        title: String,
        description: String,
        language: String,
        priority: TaskPriority,
        preferenceHelper: PreferenceHelper
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Define Channel ID based on Priority
        val channelId = when (priority) {
            TaskPriority.CRITICAL, TaskPriority.IMPORTANT -> CHANNEL_ID_HIGH
            TaskPriority.NORMAL -> CHANNEL_ID_NORMAL
            TaskPriority.CAN_WAIT -> CHANNEL_ID_LOW
        }

        createNotificationChannels(context, notificationManager, language, preferenceHelper)

        val notificationId = taskId.hashCode()

        // Action: Complete
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_COMPLETE"
            putExtra("TASK_ID", taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 1,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Snooze
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("TASK_ID", taskId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.hashCode() + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Open Task (Edit Task)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("OPEN_TASK_ID", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            taskId.hashCode() + 3,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultDesc = if (language == "ar") "تذكير صوتي أويكونوميا" else "أويكونوميا Voice Reminder"
        
        // Custom action labels as requested
        val completeActionText = if (language == "ar") "✓ إكمال" else "✓ Completed"
        val snoozeActionText = if (language == "ar") "⏰ غفوة" else "⏰ Snooze"
        val editActionText = if (language == "ar") "✏ تعديل المهمة" else "✏ Edit Task"

        val soundUri = if (preferenceHelper.soundEnabled) {
            android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.custom_notification}")
        } else null

        // Vibration patterns based on task priority
        val vibrationPattern = if (preferenceHelper.vibrationEnabled) {
            when (priority) {
                TaskPriority.CRITICAL, TaskPriority.IMPORTANT -> longArrayOf(0, 500, 200, 500, 200, 500) // Strong
                TaskPriority.NORMAL -> longArrayOf(0, 300, 150, 300) // Medium
                TaskPriority.CAN_WAIT -> longArrayOf(0, 150) // Short
            }
        } else null

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(description.ifEmpty { defaultDesc })
            .setPriority(NotificationCompat.PRIORITY_HIGH) // For Heads-Up display compatibility
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setFullScreenIntent(openPendingIntent, false) // Ensure heads-up display is triggered when screen is locked
            .addAction(android.R.drawable.checkbox_on_background, completeActionText, completePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, snoozeActionText, snoozePendingIntent)
            .addAction(android.R.drawable.ic_menu_edit, editActionText, openPendingIntent)

        if (soundUri != null) {
            builder.setSound(soundUri)
        } else {
            builder.setSilent(true)
        }

        if (vibrationPattern != null) {
            builder.setVibrate(vibrationPattern)
            // Explicitly trigger manual vibrator as a robust fallback to guarantee tactile feedback
            triggerTactileFeedback(context, vibrationPattern)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private fun triggerTactileFeedback(context: Context, pattern: LongArray) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            val vibratorManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            } else null
            val realVibrator = vibratorManager?.defaultVibrator ?: vibrator
            if (realVibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    realVibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    realVibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Tactile feedback trigger failed", e)
        }
    }

    private fun dismissNotification(context: Context, taskId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.hashCode())
    }

    private fun createNotificationChannels(
        context: Context,
        notificationManager: NotificationManager,
        language: String,
        preferenceHelper: PreferenceHelper
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = android.net.Uri.parse("android.resource://${context.packageName}/${R.raw.custom_notification}")
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            // 1. High Priority Channel
            val nameHigh = if (language == "ar") "أويكونوميا - تنبيهات هامة" else "أويكونوميا - High Priority"
            val channelHigh = NotificationChannel(
                CHANNEL_ID_HIGH,
                nameHigh,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = if (language == "ar") "تذكيرات المهام العاجلة والهامة" else "Reminders for urgent and important tasks"
                enableVibration(preferenceHelper.vibrationEnabled)
                if (preferenceHelper.vibrationEnabled) {
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                }
                if (preferenceHelper.soundEnabled) {
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
            }

            // 2. Normal Priority Channel
            val nameNormal = if (language == "ar") "أويكونوميا - تنبيهات عادية" else "أويكونوميا - Normal Priority"
            val channelNormal = NotificationChannel(
                CHANNEL_ID_NORMAL,
                nameNormal,
                NotificationManager.IMPORTANCE_HIGH // Maintain HIGH importance for heads-up
            ).apply {
                description = if (language == "ar") "تذكيرات المهام العادية واليومية" else "Reminders for daily and normal tasks"
                enableVibration(preferenceHelper.vibrationEnabled)
                if (preferenceHelper.vibrationEnabled) {
                    vibrationPattern = longArrayOf(0, 300, 150, 300)
                }
                if (preferenceHelper.soundEnabled) {
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
            }

            // 3. Low Priority Channel
            val nameLow = if (language == "ar") "أويكونوميا - تنبيهات منخفضة" else "أويكونوميا - Low Priority"
            val channelLow = NotificationChannel(
                CHANNEL_ID_LOW,
                nameLow,
                NotificationManager.IMPORTANCE_HIGH // Keep high for uniform Heads-Up behavior as requested
            ).apply {
                description = if (language == "ar") "تذكيرات المهام غير العاجلة" else "Reminders for non-urgent tasks"
                enableVibration(preferenceHelper.vibrationEnabled)
                if (preferenceHelper.vibrationEnabled) {
                    vibrationPattern = longArrayOf(0, 150)
                }
                if (preferenceHelper.soundEnabled) {
                    setSound(soundUri, audioAttributes)
                } else {
                    setSound(null, null)
                }
            }

            notificationManager.createNotificationChannel(channelHigh)
            notificationManager.createNotificationChannel(channelNormal)
            notificationManager.createNotificationChannel(channelLow)
        }
    }
}
