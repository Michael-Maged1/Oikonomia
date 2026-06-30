package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.repository.TaskRepository
import com.example.data.remote.FirebaseManager
import kotlinx.coroutines.flow.take

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Background sync started.")
        return try {
            FirebaseManager.initialize(applicationContext)
            if (FirebaseManager.isAvailable() && FirebaseManager.getCurrentUser() != null) {
                val db = AppDatabase.getDatabase(applicationContext)
                val repository = TaskRepository(db.taskDao())
                
                // 1. Sync local pending tasks to Firebase
                repository.syncPendingTasks()
                
                // 2. Fetch and merge remote changes once (take 1 emission and close)
                FirebaseManager.observeTasks().take(1).collect { remoteTasks ->
                    if (remoteTasks.isNotEmpty()) {
                        repository.mergeRemoteTasks(remoteTasks)
                    }
                }
                Result.success()
            } else {
                Log.d("SyncWorker", "Firebase not available or user not logged in. Postponing sync.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Background sync failed", e)
            Result.retry()
        }
    }
}
