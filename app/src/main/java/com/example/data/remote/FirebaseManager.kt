package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.domain.model.Task
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRepeat
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirebaseManager {
    private const val TAG = "FirebaseManager"
    
    private var isFirebaseAvailable = false
    private var auth: FirebaseAuth? = null
    private var db: FirebaseFirestore? = null
    private var remoteConfig: FirebaseRemoteConfig? = null

    fun initialize(context: Context) {
        try {
            // Check if Firebase is configured / can be initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            if (FirebaseApp.getApps(context).isEmpty()) {
                Log.i(TAG, "Firebase configuration (google-services.json) is missing. Running in offline/no-firebase mode.")
                isFirebaseAvailable = false
                return
            }
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            
            // Configure firestore offline persistence (enabled by default in Android SDK)
            
            // Initialize Remote Config
            remoteConfig = FirebaseRemoteConfig.getInstance()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            remoteConfig?.setConfigSettingsAsync(configSettings)
            remoteConfig?.setDefaultsAsync(mapOf(
                "ai_enabled" to true,
                "welcome_greeting_ar" to "مرحباً بك في أويكونوميا",
                "welcome_greeting_en" to "Welcome to أويكونوميا"
            ))
            
            isFirebaseAvailable = true
            Log.d(TAG, "Firebase initialized successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Firebase initialization failed. Running in offline/no-firebase mode.", e)
            isFirebaseAvailable = false
        }
    }

    fun isAvailable(): Boolean = isFirebaseAvailable

    // --- Authentication ---
    
    fun getCurrentUser(): FirebaseUser? {
        return if (isFirebaseAvailable) auth?.currentUser else null
    }

    fun getUserId(): String {
        return getCurrentUser()?.uid ?: "local_user"
    }

    suspend fun loginAnonymously(): FirebaseUser? {
        if (!isFirebaseAvailable) return null
        return try {
            val result = auth?.signInAnonymously()?.await()
            Log.d(TAG, "Logged in anonymously: ${result?.user?.uid}")
            result?.user
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous login failed", e)
            null
        }
    }

    suspend fun registerWithEmail(email: String, password: String): FirebaseUser? {
        if (!isFirebaseAvailable) return null
        return try {
            val result = auth?.createUserWithEmailAndPassword(email, password)?.await()
            Log.d(TAG, "Registered user: ${result?.user?.uid}")
            result?.user
        } catch (e: Exception) {
            Log.e(TAG, "Registration failed", e)
            throw e
        }
    }

    suspend fun loginWithEmail(email: String, password: String): FirebaseUser? {
        if (!isFirebaseAvailable) return null
        return try {
            val result = auth?.signInWithEmailAndPassword(email, password)?.await()
            Log.d(TAG, "Logged in user: ${result?.user?.uid}")
            result?.user
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            throw e
        }
    }

    fun logout() {
        if (isFirebaseAvailable) {
            auth?.signOut()
        }
    }

    // --- Firestore Tasks Synchronization ---

    suspend fun uploadTask(task: Task): Boolean {
        if (!isFirebaseAvailable) return false
        val uid = getUserId()
        val taskMap = mapOf(
            "id" to task.id,
            "title" to task.title,
            "description" to task.description,
            "dueDate" to task.dueDate,
            "dueTime" to task.dueTime,
            "priority" to task.priority.name,
            "category" to task.category.name,
            "repeat" to task.repeat.name,
            "isCompleted" to task.isCompleted,
            "lastUpdated" to task.lastUpdated,
            "userId" to uid
        )

        return try {
            db?.collection("users")
                ?.document(uid)
                ?.collection("tasks")
                ?.document(task.id)
                ?.set(taskMap, SetOptions.merge())
                ?.await()
            Log.d(TAG, "Task ${task.id} synced with Firestore.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to upload task to Firestore", e)
            false
        }
    }

    suspend fun deleteTask(taskId: String): Boolean {
        if (!isFirebaseAvailable) return false
        val uid = getUserId()
        return try {
            db?.collection("users")
                ?.document(uid)
                ?.collection("tasks")
                ?.document(taskId)
                ?.delete()
                ?.await()
            Log.d(TAG, "Task $taskId deleted from Firestore.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete task from Firestore", e)
            false
        }
    }

    // Observe remote changes in Firestore
    fun observeTasks(): Flow<List<Task>> = callbackFlow {
        if (!isFirebaseAvailable) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val uid = getUserId()
        val registration = db?.collection("users")
            ?.document(uid)
            ?.collection("tasks")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore task subscription failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val tasksList = mutableListOf<Task>()
                    for (doc in snapshot.documents) {
                        try {
                            val id = doc.getString("id") ?: continue
                            val title = doc.getString("title") ?: ""
                            val description = doc.getString("description") ?: ""
                            val dueDate = doc.getString("dueDate") ?: ""
                            val dueTime = doc.getString("dueTime") ?: "12:00"
                            val priorityStr = doc.getString("priority") ?: "NORMAL"
                            val categoryStr = doc.getString("category") ?: "OTHER"
                            val repeatStr = doc.getString("repeat") ?: "NONE"
                            val isCompleted = doc.getBoolean("isCompleted") ?: false
                            val lastUpdated = doc.getLong("lastUpdated") ?: System.currentTimeMillis()

                            val priority = try { TaskPriority.valueOf(priorityStr) } catch (e: Exception) { TaskPriority.NORMAL }
                            val category = try { TaskCategory.valueOf(categoryStr) } catch (e: Exception) { TaskCategory.OTHER }
                            val repeat = try { TaskRepeat.valueOf(repeatStr) } catch (e: Exception) { TaskRepeat.NONE }

                            tasksList.add(
                                Task(
                                    id = id,
                                    title = title,
                                    description = description,
                                    dueDate = dueDate,
                                    dueTime = dueTime,
                                    priority = priority,
                                    category = category,
                                    repeat = repeat,
                                    isCompleted = isCompleted,
                                    syncPending = false,
                                    lastUpdated = lastUpdated,
                                    userId = uid
                                )
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing Firestore document", e)
                        }
                    }
                    trySend(tasksList)
                }
            }

        awaitClose {
            registration?.remove()
        }
    }

    // --- User Settings Syncing ---
    suspend fun saveUserSettings(nickname: String, language: String, theme: String, defaultReminderTime: String): Boolean {
        if (!isFirebaseAvailable) return false
        val uid = getUserId()
        val settingsMap = mapOf(
            "nickname" to nickname,
            "language" to language,
            "theme" to theme,
            "defaultReminderTime" to defaultReminderTime,
            "lastUpdated" to System.currentTimeMillis()
        )
        return try {
            db?.collection("users")
                ?.document(uid)
                ?.collection("settings")
                ?.document("user_profile")
                ?.set(settingsMap, SetOptions.merge())
                ?.await()
            Log.d(TAG, "User settings synced with Firestore.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user settings to Firestore", e)
            false
        }
    }

    fun observeUserSettings(onSettingsChanged: (nickname: String, language: String, theme: String, defaultReminderTime: String) -> Unit) {
        if (!isFirebaseAvailable) return
        val uid = getUserId()
        db?.collection("users")
            ?.document(uid)
            ?.collection("settings")
            ?.document("user_profile")
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to listen to settings changes", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val nickname = snapshot.getString("nickname") ?: ""
                    val language = snapshot.getString("language") ?: "ar"
                    val theme = snapshot.getString("theme") ?: "dark"
                    val defaultReminderTime = snapshot.getString("defaultReminderTime") ?: "12:00"
                    onSettingsChanged(nickname, language, theme, defaultReminderTime)
                }
            }
    }

    // --- Remote Config ---
    fun isAiEnabled(): Boolean {
        return remoteConfig?.getBoolean("ai_enabled") ?: true
    }

    fun getWelcomeGreeting(language: String): String {
        return if (language == "ar") {
            remoteConfig?.getString("welcome_greeting_ar") ?: "مرحباً بك في أويكونوميا"
        } else {
            remoteConfig?.getString("welcome_greeting_en") ?: "Welcome to أويكونوميا"
        }
    }

    suspend fun fetchRemoteConfig() {
        if (!isFirebaseAvailable) return
        try {
            remoteConfig?.fetchAndActivate()?.await()
            Log.d(TAG, "Remote Config fetched and activated.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch Remote Config", e)
        }
    }
}
