package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.concurrent.ConcurrentHashMap

class SpeechManager private constructor(private val context: Context) {

    companion object {
        private const val TAG = "SpeechManager"
        
        @Volatile
        private var instance: SpeechManager? = null

        fun getInstance(context: Context): SpeechManager {
            return instance ?: synchronized(this) {
                instance ?: SpeechManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var currentLanguage = "ar"
    private var currentUseOffline = false

    // Cache to check if offline language packs exist (ar-EG, en-US)
    private val offlineSupportCache = ConcurrentHashMap<String, Boolean>()

    // Callbacks
    private var onStartCallback: (() -> Unit)? = null
    private var onPartialResultCallback: ((String) -> Unit)? = null
    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((Int, String) -> Unit)? = null

    init {
        // Pre-warm the cache for both English and Arabic
        prewarmOfflineCache()
    }

    private fun prewarmOfflineCache() {
        checkOfflineSupportForLang("ar")
        checkOfflineSupportForLang("en")
    }

    private fun checkOfflineSupportForLang(lang: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val tempRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang == "ar") "ar-EG" else "en-US")
                }
                tempRecognizer.checkRecognitionSupport(
                    intent,
                    ContextCompat.getMainExecutor(context),
                    object : android.speech.RecognitionSupportCallback {
                        override fun onSupportResult(recognitionSupport: android.speech.RecognitionSupport) {
                            val targetLang = if (lang == "ar") "ar-EG" else "en-US"
                            val targetLangShort = if (lang == "ar") "ar" else "en"
                            val isInstalled = recognitionSupport.installedOnDeviceLanguages.any {
                                it.equals(targetLang, ignoreCase = true) || it.equals(targetLangShort, ignoreCase = true)
                            }
                            Log.d(TAG, "Offline cache prewarm for $lang: installed=$isInstalled")
                            offlineSupportCache[lang] = isInstalled
                            try {
                                tempRecognizer.destroy()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error destroying temp recognizer", e)
                            }
                        }

                        override fun onError(error: Int) {
                            Log.e(TAG, "checkRecognitionSupport error for $lang: $error")
                            offlineSupportCache[lang] = false
                            try {
                                tempRecognizer.destroy()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error destroying temp recognizer", e)
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed checking recognition support for $lang", e)
                offlineSupportCache[lang] = false
            }
        } else {
            // Older versions (Android 12)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val available = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                    offlineSupportCache[lang] = available
                } catch (e: Exception) {
                    offlineSupportCache[lang] = false
                }
            } else {
                offlineSupportCache[lang] = false
            }
        }
    }

    private fun initializeRecognizer(useOffline: Boolean) {
        // Destroy existing instance to avoid leaks and concurrent sessions
        cleanupRecognizer()

        val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        Log.d(TAG, "Initializing SpeechRecognizer: available=$isAvailable, useOffline=$useOffline")

        if (!isAvailable) {
            return
        }

        try {
            speechRecognizer = if (useOffline && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Log.d(TAG, "Creating On-Device Speech Recognizer")
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            } else {
                Log.d(TAG, "Creating Standard Speech Recognizer")
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer (offline=$useOffline), falling back to standard", e)
            speechRecognizer = try {
                SpeechRecognizer.createSpeechRecognizer(context)
            } catch (ex: Exception) {
                Log.e(TAG, "Critical failure: standard SpeechRecognizer creation failed", ex)
                null
            }
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                isListening = true
                onStartCallback?.invoke()
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = getErrorMessage(error)
                Log.e(TAG, "onError: $error - $errorMsg (currentUseOffline=$currentUseOffline)")

                // Fallback mechanism: if we tried offline and failed, let's fall back to online immediately
                if (currentUseOffline) {
                    Log.w(TAG, "Offline speech recognition failed. Falling back to online recognition...")
                    currentUseOffline = false
                    // Start standard/online recognition on the main thread
                    ContextCompat.getMainExecutor(context).execute {
                        startListeningWithFallback(currentLanguage, useOffline = false)
                    }
                    return
                }

                // If fallback failed or we were already on online, report error
                onErrorCallback?.invoke(error, errorMsg)
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val finalResult = matches?.firstOrNull() ?: ""
                Log.d(TAG, "onResults: $finalResult")
                onFinalResultCallback?.invoke(finalResult)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val partialResult = matches?.firstOrNull() ?: ""
                if (partialResult.isNotEmpty()) {
                    Log.d(TAG, "onPartialResults: $partialResult")
                    onPartialResultCallback?.invoke(partialResult)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    fun isOfflineSupported(language: String): Boolean {
        return offlineSupportCache[language] ?: false
    }

    fun triggerOfflineDownload(
        language: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                val langTag = if (language == "ar") "ar-EG" else "en-US"
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                }
                val executor = ContextCompat.getMainExecutor(context)
                
                recognizer.triggerModelDownload(
                    intent,
                    executor,
                    object : android.speech.ModelDownloadListener {
                        override fun onProgress(progress: Int) {
                            Log.d(TAG, "Download progress for $language: $progress%")
                            onProgress(progress)
                        }

                        override fun onSuccess() {
                            Log.d(TAG, "Download succeeded for $language")
                            offlineSupportCache[language] = true
                            onSuccess()
                            try {
                                recognizer.destroy()
                            } catch (e: Exception) {}
                        }

                        override fun onScheduled() {
                            Log.d(TAG, "Download scheduled for $language")
                        }

                        override fun onError(error: Int) {
                            Log.e(TAG, "Download error for $language: $error")
                            val userFriendlyMsg = if (error == 12) { // SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                                if (language == "ar") {
                                    "حزمة اللغة غير مدعومة للتعرف الصوتي دون اتصال بشكل افتراضي على هذا الجهاز. (رمز الخطأ: 12)\n\nلحل هذه المشكلة وتفعيل ميزة التعرف الصوتي دون إنترنت:\n\n1️⃣ قم بتحديث تطبيق 'Google' وتطبيق 'خدمات الصوت من Google' من متجر Google Play إلى أحدث إصدار.\n\n2️⃣ اذهب إلى إعدادات الهاتف (Settings) ⬅️ اللغات والإدخال (Languages & Input) ⬅️ خدمات الصوت من Google (Speech Services by Google) ⬅️ التعرف على الكلام دون اتصال (Offline speech recognition)، وابحث عن اللغة العربية وقم بتنزيلها يدوياً.\n\n3️⃣ تأكد من تعيين محرك خدمات جوجل الصوتي كمحرك افتراضي للكلام بالصوت."
                                } else {
                                    "This language is not supported for offline speech recognition on this device by default. (Error Code: 12)\n\nTo resolve this and enable offline voice recognition:\n\n1️⃣ Update the 'Google' app and 'Speech Services by Google' from the Google Play Store to the latest version.\n\n2️⃣ Go to your Android Settings ⬅️ Languages & Input ⬅️ Speech Services by Google ⬅️ Offline speech recognition, find your language, and download it manually.\n\n3️⃣ Ensure that Google Speech Engine is set as your default voice provider."
                                }
                            } else {
                                if (language == "ar") {
                                    "حدث خطأ أثناء تحميل حزمة اللغة. (رمز الخطأ: $error)"
                                } else {
                                    "An error occurred while downloading the language pack. (Error Code: $error)"
                                }
                            }
                            onError(userFriendlyMsg)
                            try {
                                recognizer.destroy()
                            } catch (e: Exception) {}
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "triggerOfflineDownload failed", e)
                onError(e.localizedMessage ?: "Failed to trigger download")
            }
        } else {
            onError("Offline model download is only supported on Android 14 (API 34) and above.")
        }
    }

    fun startListening(
        language: String, // "ar" or "en"
        onStart: () -> Unit,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (Int, String) -> Unit
    ) {
        this.onStartCallback = onStart
        this.onPartialResultCallback = onPartialResult
        this.onFinalResultCallback = onFinalResult
        this.onErrorCallback = onError
        this.currentLanguage = language

        // Decide if we should try offline first
        val hasOfflineSupport = offlineSupportCache[language] ?: false
        this.currentUseOffline = hasOfflineSupport

        Log.d(TAG, "startListening: language=$language, preferredOffline=$hasOfflineSupport")
        startListeningWithFallback(language, useOffline = hasOfflineSupport)
    }

    private fun startListeningWithFallback(language: String, useOffline: Boolean) {
        initializeRecognizer(useOffline)

        if (speechRecognizer == null) {
            val errMsg = if (language == "ar") "التسجيل الصوتي غير مدعوم على هذا الجهاز." else "Speech recognition is not supported on this device."
            onErrorCallback?.invoke(SpeechRecognizer.ERROR_CLIENT, errMsg)
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            
            // Preferred Language: Arabic (Egypt) vs English (US)
            val langTag = if (language == "ar") "ar-EG" else "en-US"
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
            
            // Dialects and English supported dynamically
            putExtra(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES, arrayListOf("ar-EG", "ar-SA", "en-US"))
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            
            // Request partial results for live transcription
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

            // Configure offline preference
            if (useOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }

            // Modern Android (API 34+) Language Detection & Switching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                putExtra("android.speech.extra.LANGUAGE_DETECTION_ALLOWED", true)
                putExtra("android.speech.extra.LANGUAGE_SWITCH_ALLOWED", true)
            }
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting SpeechRecognizer", e)
            if (useOffline) {
                Log.w(TAG, "On-device start failed, trying standard fallback...")
                currentUseOffline = false
                startListeningWithFallback(language, useOffline = false)
            } else {
                onErrorCallback?.invoke(SpeechRecognizer.ERROR_CLIENT, e.localizedMessage ?: "Start listening failed")
            }
        }
    }

    fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopListening", e)
            }
            isListening = false
        }
    }

    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelListening", e)
        }
        isListening = false
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.setRecognitionListener(null)
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up recognizer", e)
        }
        speechRecognizer = null
        isListening = false
    }

    fun destroy() {
        cleanupRecognizer()
        onStartCallback = null
        onPartialResultCallback = null
        onFinalResultCallback = null
        onErrorCallback = null
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Speech recognition error"
        }
    }
}
