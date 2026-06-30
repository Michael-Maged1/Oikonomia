package com.example.presentation.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PreferenceHelper
import com.example.data.local.toDomain
import com.example.data.remote.FirebaseManager
import com.example.data.remote.GeminiClient
import com.example.data.remote.ParsedTaskResult
import com.example.data.remote.OfflineTaskParser
import com.example.data.repository.TaskRepository
import com.example.domain.model.Task
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRepeat
import com.example.notifications.ReminderScheduler
import com.google.firebase.auth.FirebaseUser
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class SopViewModel(
    application: Application,
    private val repository: TaskRepository
) : AndroidViewModel(application) {

    private val TAG = "SopViewModel"
    private val prefs = PreferenceHelper(application)

    // --- Onboarding / Prefs State ---
    private val _isFirstLaunch = MutableStateFlow(prefs.isFirstLaunch)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    private val _nickname = MutableStateFlow(prefs.nickname)
    val nickname: StateFlow<String> = _nickname.asStateFlow()

    private val _language = MutableStateFlow(prefs.language)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _theme = MutableStateFlow(prefs.theme)
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _defaultReminderTime = MutableStateFlow(prefs.defaultReminderTime)
    val defaultReminderTime: StateFlow<String> = _defaultReminderTime.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(prefs.vibrationEnabled)
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.soundEnabled)
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _notificationVolume = MutableStateFlow(prefs.notificationVolume)
    val notificationVolume: StateFlow<Int> = _notificationVolume.asStateFlow()

    private val _snoozeDuration = MutableStateFlow(prefs.snoozeDuration)
    val snoozeDuration: StateFlow<Int> = _snoozeDuration.asStateFlow()

    fun updateVibrationEnabled(enabled: Boolean) {
        prefs.vibrationEnabled = enabled
        _vibrationEnabled.value = enabled
    }

    fun updateSoundEnabled(enabled: Boolean) {
        prefs.soundEnabled = enabled
        _soundEnabled.value = enabled
    }

    fun updateNotificationVolume(volume: Int) {
        prefs.notificationVolume = volume
        _notificationVolume.value = volume
    }

    fun updateSnoozeDuration(duration: Int) {
        prefs.snoozeDuration = duration
        _snoozeDuration.value = duration
    }

    // --- Task List State ---
    val tasks: StateFlow<List<Task>> = repository.tasksFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- Voice UI State ---
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _voiceUiState = MutableStateFlow<VoiceUiState>(VoiceUiState.Idle)
    val voiceUiState: StateFlow<VoiceUiState> = _voiceUiState.asStateFlow()

    // --- Pre-filled / fallback speech text ---
    private val _partialVoiceInputText = MutableStateFlow("")
    val partialVoiceInputText: StateFlow<String> = _partialVoiceInputText.asStateFlow()

    fun setPartialVoiceInputText(text: String) {
        _partialVoiceInputText.value = text
    }

    fun clearPartialVoiceInputText() {
        _partialVoiceInputText.value = ""
    }

    // --- Auth State ---
    private val _currentUser = MutableStateFlow<FirebaseUser?>(FirebaseManager.getCurrentUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    init {
        // Initialize Firebase
        FirebaseManager.initialize(application)
        _currentUser.value = FirebaseManager.getCurrentUser()
        
        // Start background syncing with Firestore if available
        if (FirebaseManager.isAvailable()) {
            repository.startFirebaseSync()
            schedulePeriodicSync()
            viewModelScope.launch {
                FirebaseManager.fetchRemoteConfig()
            }
            FirebaseManager.observeUserSettings { nick, lang, th, rem ->
                if (nick.isNotEmpty()) {
                    prefs.nickname = nick
                    _nickname.value = nick
                }
                if (lang.isNotEmpty()) {
                    prefs.language = lang
                    _language.value = lang
                }
                if (th.isNotEmpty()) {
                    prefs.theme = th
                    _theme.value = th
                }
                if (rem.isNotEmpty()) {
                    prefs.defaultReminderTime = rem
                    _defaultReminderTime.value = rem
                }
            }
        }
    }

    // --- Onboarding & Pref Actions ---
    fun finishOnboarding(userNickname: String, userLang: String) {
        prefs.nickname = userNickname
        prefs.language = userLang
        prefs.isFirstLaunch = false
        
        _nickname.value = userNickname
        _language.value = userLang
        _isFirstLaunch.value = false
        
        // Log in anonymously to enable silent sync
        viewModelScope.launch {
            if (FirebaseManager.isAvailable()) {
                val user = FirebaseManager.loginAnonymously()
                _currentUser.value = user
                repository.startFirebaseSync()
                // Sync settings to Firestore
                FirebaseManager.saveUserSettings(userNickname, userLang, _theme.value, _defaultReminderTime.value)
            }
        }
    }

    fun updateLanguage(lang: String) {
        prefs.language = lang
        _language.value = lang
        viewModelScope.launch {
            FirebaseManager.saveUserSettings(_nickname.value, lang, _theme.value, _defaultReminderTime.value)
        }
    }

    fun updateTheme(themeStr: String) {
        prefs.theme = themeStr
        _theme.value = themeStr
        viewModelScope.launch {
            FirebaseManager.saveUserSettings(_nickname.value, _language.value, themeStr, _defaultReminderTime.value)
        }
    }

    fun updateNickname(name: String) {
        prefs.nickname = name
        _nickname.value = name
        viewModelScope.launch {
            FirebaseManager.saveUserSettings(name, _language.value, _theme.value, _defaultReminderTime.value)
        }
    }

    fun updateDefaultReminderTime(timeStr: String) {
        prefs.defaultReminderTime = timeStr
        _defaultReminderTime.value = timeStr
        viewModelScope.launch {
            FirebaseManager.saveUserSettings(_nickname.value, _language.value, _theme.value, timeStr)
        }
    }

    fun formatToArabicDigits(number: Int): String {
        val numberStr = number.toString()
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val builder = StringBuilder()
        for (ch in numberStr) {
            if (ch in '0'..'9') {
                builder.append(arabicDigits[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    // --- Personalized Greeting Generation ---
    fun getAssistantGreeting(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val name = _nickname.value.ifEmpty { "Michael" }
        val lang = _language.value

        val timeGreeting = if (lang == "ar") {
            when (hour) {
                in 5..11 -> {
                    val greetings = listOf(
                        "صباح الخير يا $name ☀️",
                        "صباح المودة والسعادة يا $name 👋",
                        "صباح الخير والبركة يا $name ✨"
                    )
                    greetings.random()
                }
                in 12..16 -> {
                    val greetings = listOf(
                        "مساء الخير يا $name 👋",
                        "مرحباً بعودتك يا $name ❤️",
                        "أتمنى أن يكون يومك رائعاً وممتعاً يا $name ✨"
                    )
                    greetings.random()
                }
                in 17..20 -> {
                    val greetings = listOf(
                        "مساء الخير يا $name 🌟",
                        "مرحباً بعودتك يا $name ❤️",
                        "أتمنى لك أمسية رائعة وممتعة يا $name ✨"
                    )
                    greetings.random()
                }
                else -> {
                    val greetings = listOf(
                        "مساء هادئ يا $name 🌙",
                        "أتمنى لك وقتاً هادئاً ومريحاً يا $name ✨",
                        "مرحباً بك يا $name ❤️"
                    )
                    greetings.random()
                }
            }
        } else {
            when (hour) {
                in 5..11 -> "Good morning, $name ☀️"
                in 12..16 -> "Good afternoon, $name 👋"
                in 17..20 -> "Good evening, $name 🌟"
                else -> "Hello, $name ❤️"
            }
        }

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayTasks = tasks.value.filter { it.dueDate == todayStr }
        val remainingTasks = todayTasks.filter { !it.isCompleted }.size
        val totalTasks = todayTasks.size

        return if (lang == "ar") {
            val easternRemaining = formatToArabicDigits(remainingTasks)
            when {
                totalTasks == 0 -> {
                    "$timeGreeting\nليس لديك مهام مجدولة اليوم. ما المهمة التي تريد مني أن أتذكرها لك؟"
                }
                remainingTasks == 0 -> {
                    "$timeGreeting\nأحسنت يا $name! لقد أنهيت جميع مهام اليوم 🎉"
                }
                else -> {
                    "$timeGreeting\nاليوم لديك $easternRemaining مهام متبقية. ما المهمة التالية التي تريد مني أن أتذكرها لك؟"
                }
            }
        } else {
            when {
                totalTasks == 0 -> {
                    "$timeGreeting\nYou have no tasks scheduled today. What would you like me to remember for you?"
                }
                remainingTasks == 0 -> {
                    "Great job, $name! You've completed all tasks for today 🎉"
                }
                else -> {
                    "$timeGreeting\nYou have $remainingTasks tasks remaining today."
                }
            }
        }
    }

    // --- Connectivity Check ---
    fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        if (connectivityManager != null) {
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
        return false
    }

    // --- Voice Processing Logic ---
    fun startVoiceRecording() {
        _isRecording.value = true
        _voiceUiState.value = VoiceUiState.Recording
    }

    fun stopVoiceRecording(spokenText: String) {
        _isRecording.value = false
        if (spokenText.trim().isEmpty()) {
            _voiceUiState.value = VoiceUiState.Idle
            return
        }

        _voiceUiState.value = VoiceUiState.Processing
        viewModelScope.launch {
            val currentDateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm EEEE", Locale.US).format(Date())
            var parsedResult: ParsedTaskResult? = null
            
            if (isNetworkAvailable()) {
                try {
                    parsedResult = GeminiClient.parseTaskFromText(spokenText, currentDateTimeStr)
                } catch (e: Exception) {
                    Log.e("SopViewModel", "Gemini parsing failed", e)
                }
            } else {
                Log.d("SopViewModel", "No internet connection. Skipping Gemini, using Offline Parser.")
            }
            
            // Offline/Gemini failure fallback
            if (parsedResult == null) {
                try {
                    Log.d("SopViewModel", "Gemini analysis was offline or failed. Falling back to Offline Rule-Based Parser.")
                    parsedResult = OfflineTaskParser.parse(spokenText)
                } catch (e: Exception) {
                    Log.e("SopViewModel", "Offline parser failed", e)
                }
            }
            
            if (parsedResult != null) {
                _voiceUiState.value = VoiceUiState.Preview(parsedResult)
            } else {
                _voiceUiState.value = VoiceUiState.Error(
                    if (_language.value == "ar") "عذراً، لم أتمكن من معالجة طلبك." else "Sorry, I couldn't understand that."
                )
            }
        }
    }

    fun cancelVoicePreview() {
        _voiceUiState.value = VoiceUiState.Idle
    }

    fun saveTaskFromPreview(
        title: String,
        description: String,
        dueDate: String,
        dueTime: String,
        priority: TaskPriority,
        category: TaskCategory,
        repeat: TaskRepeat
    ) {
        viewModelScope.launch {
            val task = Task(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                dueDate = dueDate.ifEmpty { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) },
                dueTime = dueTime.ifEmpty {
                    val defTime = _defaultReminderTime.value
                    if (defTime == "Off") "12:00" else defTime
                },
                priority = priority,
                category = category,
                repeat = repeat,
                isCompleted = false,
                lastUpdated = System.currentTimeMillis(),
                userId = FirebaseManager.getUserId()
            )
            repository.saveTask(task)
            ReminderScheduler.scheduleReminder(getApplication(), task)
            _voiceUiState.value = VoiceUiState.Idle
        }
    }

    // --- Manual Task Actions ---
    fun saveTaskManual(
        id: String?,
        title: String,
        description: String,
        dueDate: String,
        dueTime: String,
        priority: TaskPriority,
        category: TaskCategory,
        repeat: TaskRepeat
    ) {
        viewModelScope.launch {
            val task = Task(
                id = id ?: UUID.randomUUID().toString(),
                title = title,
                description = description,
                dueDate = dueDate,
                dueTime = dueTime.ifEmpty {
                    val defTime = _defaultReminderTime.value
                    if (defTime == "Off") "12:00" else defTime
                },
                priority = priority,
                category = category,
                repeat = repeat,
                isCompleted = false,
                lastUpdated = System.currentTimeMillis(),
                userId = FirebaseManager.getUserId()
            )
            repository.saveTask(task)
            ReminderScheduler.scheduleReminder(getApplication(), task)
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                isCompleted = !task.isCompleted,
                lastUpdated = System.currentTimeMillis()
            )
            repository.saveTask(updatedTask)
            if (updatedTask.isCompleted) {
                ReminderScheduler.cancelReminder(getApplication(), task.id)
            } else {
                ReminderScheduler.scheduleReminder(getApplication(), updatedTask)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task.id)
            ReminderScheduler.cancelReminder(getApplication(), task.id)
        }
    }

    // --- Gemini Fill Helper for Manual Screen ---
    fun autofillTaskWithAi(userSpeech: String, onFilled: (ParsedTaskResult) -> Unit) {
        viewModelScope.launch {
            val currentDateTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm EEEE", Locale.US).format(Date())
            val parsedResult = GeminiClient.parseTaskFromText(userSpeech, currentDateTimeStr)
            if (parsedResult != null) {
                onFilled(parsedResult)
            }
        }
    }

    // --- Sync Action ---
    fun triggerManualSync() {
        viewModelScope.launch {
            if (FirebaseManager.isAvailable()) {
                repository.syncPendingTasks()
            }
        }
    }

    // --- Firebase Auth Actions ---
    fun linkWithEmail(email: String, pw: String, onResult: (Boolean) -> Unit) {
        _authError.value = null
        viewModelScope.launch {
            try {
                val user = FirebaseManager.registerWithEmail(email, pw)
                if (user != null) {
                    _currentUser.value = user
                    repository.startFirebaseSync()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Authentication failed"
                onResult(false)
            }
        }
    }

    fun loginEmail(email: String, pw: String, onResult: (Boolean) -> Unit) {
        _authError.value = null
        viewModelScope.launch {
            try {
                val user = FirebaseManager.loginWithEmail(email, pw)
                if (user != null) {
                    _currentUser.value = user
                    repository.startFirebaseSync()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Authentication failed"
                onResult(false)
            }
        }
    }

    fun mergeLocalTasksToNewUser(newUid: String) {
        viewModelScope.launch {
            // Get all tasks currently in Room
            val localTasks = repository.tasksFlow.stateIn(viewModelScope).value
            if (localTasks.isNotEmpty()) {
                Log.d(TAG, "Merging ${localTasks.size} local tasks to new user ID: $newUid")
                for (task in localTasks) {
                    val updatedTask = task.copy(
                        userId = newUid,
                        syncPending = true,
                        lastUpdated = System.currentTimeMillis()
                    )
                    // Save to Room which will automatically trigger upload or schedule it
                    repository.saveTask(updatedTask)
                }
                // Trigger immediate sync of all pending items
                repository.syncPendingTasks()
            }
            // Resume/Restart Firestore listener for the new UID
            repository.startFirebaseSync()
        }
    }

    fun signInWithGoogle(context: Context, onResult: (Boolean) -> Unit) {
        _authError.value = null
        viewModelScope.launch {
            try {
                // Check if Google Web Client ID is still set to the default placeholder
                val webClientId = context.getString(com.example.R.string.default_web_client_id)
                val isAr = _language.value == "ar"
                
                if (webClientId == "your-web-client-id.apps.googleusercontent.com" || webClientId.trim().isEmpty()) {
                    val errorMsg = if (isAr) {
                        "يرجى استبدال 'your-web-client-id.apps.googleusercontent.com' بمعرف عميل Google Web Client ID الحقيقي الخاص بك في ملف strings.xml وربط بصمة SHA-1 في وحدة تحكم Firebase لتفعيل الدخول بواسطة جوجل."
                    } else {
                        "Please replace 'your-web-client-id.apps.googleusercontent.com' with your actual Google Web Client ID in strings.xml and register your SHA-1 in Firebase Console."
                    }
                    _authError.value = errorMsg
                    onResult(false)
                    return@launch
                }

                val credentialManager = CredentialManager.create(context)
                
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                val credentialResult = result.credential
                if (credentialResult is GoogleIdTokenCredential) {
                    val idToken = credentialResult.idToken
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    
                    if (!FirebaseManager.isAvailable()) {
                        _authError.value = if (isAr) "خدمة قاعدة البيانات الفايربيز غير متوفرة حالياً" else "Firebase is not available (offline mode)"
                        onResult(false)
                        return@launch
                    }
                    val authInstance = com.google.firebase.auth.FirebaseAuth.getInstance()
                    val currentUser = authInstance.currentUser
                    
                    if (currentUser != null && currentUser.isAnonymous) {
                        try {
                            val linkResult = currentUser.linkWithCredential(firebaseCredential).await()
                            val linkedUser = linkResult.user
                            if (linkedUser != null) {
                                Log.d(TAG, "Successfully linked anonymous account to Google: ${linkedUser.uid}")
                                _currentUser.value = linkedUser
                                mergeLocalTasksToNewUser(linkedUser.uid)
                                onResult(true)
                                return@launch
                            }
                        } catch (linkException: Exception) {
                            Log.e(TAG, "Linking failed, falling back to direct sign-in", linkException)
                        }
                    }
                    
                    // Direct sign in fallback/default
                    val signInResult = authInstance.signInWithCredential(firebaseCredential).await()
                    val signedInUser = signInResult.user
                    if (signedInUser != null) {
                        Log.d(TAG, "Successfully signed in with Google: ${signedInUser.uid}")
                        _currentUser.value = signedInUser
                        mergeLocalTasksToNewUser(signedInUser.uid)
                        onResult(true)
                    } else {
                        onResult(false)
                    }
                } else {
                    Log.e(TAG, "Unexpected credential type: ${credentialResult.type}")
                    _authError.value = if (isAr) "نوع مصادقة غير متوقع" else "Unexpected credential type"
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Google Sign-In failed", e)
                val isAr = _language.value == "ar"
                val defaultError = e.localizedMessage ?: "Google Sign-In failed"
                val friendlyError = if (e.javaClass.simpleName == "NoCredentialException") {
                    if (isAr) {
                        "لم يتم العثور على حساب Google على هذا الجهاز. يرجى التأكد من إضافة حساب Google نشط."
                    } else {
                        "No Google account found on this device. Please ensure you have added a Google account."
                    }
                } else if (e.message?.contains("7:") == true || e.message?.contains("developer") == true || e.javaClass.simpleName.contains("Developer") || e.message?.contains("DEVELOPER_ERROR") == true) {
                    if (isAr) {
                        "خطأ مطور (Developer Error 10/7). تأكد من إعداد معرف عميل الويب بشكل صحيح وربط بصمات SHA-1 و SHA-256 في إعدادات مشروع Firebase."
                    } else {
                        "Developer configuration error (Code 10/7). Please ensure your SHA-1 and SHA-256 fingerprints are added correctly in the Firebase project settings."
                    }
                } else {
                    defaultError
                }
                _authError.value = friendlyError
                onResult(false)
            }
        }
    }

    fun performLogout() {
        FirebaseManager.logout()
        _currentUser.value = null
    }

    /*
     * =========================================================================
     * CRITICAL WARNING: GOOGLE SIGN-IN & FIREBASE AUTHENTICATION CONFIGURATION
     * =========================================================================
     * For Google Sign-In via Credential Manager to function correctly, you MUST
     * generate the SHA-1 and SHA-256 fingerprint of your debug & release keys
     * and add them to your Android App inside the Firebase Console Project Settings.
     *
     * How to get SHA fingerprints in this project:
     * 1. Open the terminal or Gradle panel in Android Studio.
     * 2. Run the Gradle signingReport task:
     *    `gradle signingReport`
     * 3. Locate the "debug" and "release" configurations in the output.
     * 4. Copy the SHA-1 and SHA-256 hex strings.
     * 5. Go to Firebase Console -> Project Settings -> Your Apps -> Add Fingerprint.
     * 6. Paste the SHA-1 and SHA-256 values.
     *
     * NOTE: If using Google Play App Signing, copy the SHA fingerprint from the
     * Google Play Console (Setup -> App integrity) and add it to the Firebase Console too.
     * =========================================================================
     */

    fun deleteUserAccount(context: Context, onResult: (Boolean) -> Unit) {
        _authError.value = null
        viewModelScope.launch {
            try {
                if (!FirebaseManager.isAvailable()) {
                    _authError.value = "Firebase is not available (offline mode)"
                    onResult(false)
                    return@launch
                }
                val firebaseAuth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val currentUser = firebaseAuth.currentUser
                if (currentUser != null) {
                    currentUser.delete().await()
                    Log.d(TAG, "Successfully deleted user account from Firebase")
                    _currentUser.value = null
                    FirebaseManager.logout()
                    onResult(true)
                } else {
                    onResult(false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Account deletion failed", e)
                _authError.value = e.localizedMessage ?: "Account deletion failed"
                onResult(false)
            }
        }
    }

    fun schedulePeriodicSync() {
        try {
            val constraints = androidx.work.Constraints.Builder()
                .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                .build()

            val syncRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.sync.SyncWorker>(
                1, java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            androidx.work.WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
                "OikonomiaSyncWork",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
            Log.d(TAG, "SyncWorker periodic work scheduled successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule SyncWorker periodic work", e)
        }
    }

    fun triggerSyncNow() {
        viewModelScope.launch {
            if (FirebaseManager.isAvailable()) {
                repository.syncPendingTasks()
                // Fetch latest remote changes and merge
                try {
                    com.example.data.remote.FirebaseManager.observeTasks().take(1).collect { remoteTasks ->
                        if (remoteTasks.isNotEmpty()) {
                            repository.mergeRemoteTasks(remoteTasks)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Immediate sync pull failed", e)
                }
            }
        }
    }
}

sealed interface VoiceUiState {
    object Idle : VoiceUiState
    object Recording : VoiceUiState
    object Processing : VoiceUiState
    data class Preview(val parsedTask: ParsedTaskResult) : VoiceUiState
    data class Error(val message: String) : VoiceUiState
}

class SopViewModelFactory(
    private val application: Application,
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SopViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SopViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
