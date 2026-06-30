package com.example.data.repository

import android.util.Log
import com.example.data.local.TaskDao
import com.example.data.local.toEntity
import com.example.data.local.toDomain
import com.example.data.remote.FirebaseManager
import com.example.domain.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskRepository(
    private val taskDao: TaskDao,
    private val externalScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val TAG = "TaskRepository"

    // Live Flow of local tasks, always our source of truth
    val tasksFlow: Flow<List<Task>> = taskDao.getAllTasks().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getTaskById(id: String): Task? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)?.toDomain()
    }

    // Insert or update task
    suspend fun saveTask(task: Task) = withContext(Dispatchers.IO) {
        // 1. Save locally first with syncPending = true
        val taskToSaveLocally = task.copy(
            syncPending = true,
            lastUpdated = System.currentTimeMillis()
        )
        taskDao.insertTask(taskToSaveLocally.toEntity())

        // 2. Sync silently with Firebase
        externalScope.launch {
            if (FirebaseManager.isAvailable()) {
                val success = FirebaseManager.uploadTask(taskToSaveLocally)
                if (success) {
                    // Update local sync status to false
                    val syncedTask = taskToSaveLocally.copy(syncPending = false)
                    taskDao.insertTask(syncedTask.toEntity())
                }
            }
        }
    }

    // Delete task
    suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        // 1. Delete locally
        taskDao.deleteTaskById(taskId)

        // 2. Delete remotely
        externalScope.launch {
            if (FirebaseManager.isAvailable()) {
                FirebaseManager.deleteTask(taskId)
            }
        }
    }

    // Check offline items and sync them
    suspend fun syncPendingTasks() = withContext(Dispatchers.IO) {
        if (!FirebaseManager.isAvailable()) return@withContext
        val pending = taskDao.getSyncPendingTasks()
        if (pending.isEmpty()) return@withContext

        Log.d(TAG, "Syncing ${pending.size} pending tasks to Firestore...")
        for (entity in pending) {
            val domainTask = entity.toDomain()
            val success = FirebaseManager.uploadTask(domainTask)
            if (success) {
                taskDao.insertTask(entity.copy(syncPending = false))
            }
        }
    }

    // Merge changes from Firestore into Room database
    suspend fun mergeRemoteTasks(remoteTasks: List<Task>) = withContext(Dispatchers.IO) {
        for (remoteTask in remoteTasks) {
            val localTaskEntity = taskDao.getTaskById(remoteTask.id)
            if (localTaskEntity == null) {
                // Not in local DB, insert it
                taskDao.insertTask(remoteTask.toEntity())
            } else {
                val localTask = localTaskEntity.toDomain()
                // Conflict resolution: use lastUpdated timestamp
                if (remoteTask.lastUpdated > localTask.lastUpdated) {
                    // Remote is newer, update local Room DB
                    taskDao.insertTask(remoteTask.toEntity())
                } else if (localTask.lastUpdated > remoteTask.lastUpdated || localTask.syncPending) {
                    // Local is newer, upload to Firestore
                    FirebaseManager.uploadTask(localTask)
                    taskDao.insertTask(localTaskEntity.copy(syncPending = false))
                }
            }
        }
    }

    private var syncJob: kotlinx.coroutines.Job? = null

    // Synchronize both directions
    fun startFirebaseSync() {
        if (!FirebaseManager.isAvailable()) return
        syncJob?.cancel()
        syncJob = externalScope.launch {
            // First push pending
            syncPendingTasks()

            // Then observe and merge remote changes
            FirebaseManager.observeTasks().collect { remoteTasks ->
                if (remoteTasks.isNotEmpty()) {
                    mergeRemoteTasks(remoteTasks)
                }
            }
        }
    }
}
