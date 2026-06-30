package com.example.presentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ParsedTaskResult
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRepeat
import com.example.presentation.viewmodel.SopViewModel
import com.example.presentation.viewmodel.VoiceUiState
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Bundle
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainVoiceScreen(
    viewModel: SopViewModel,
    onNavigateToTasks: () -> Unit,
    onNavigateToCreate: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val voiceUiState by viewModel.voiceUiState.collectAsState()
    
    var localVoiceText by remember { mutableStateOf("") }
    val assistantGreeting = remember(viewModel.tasks.value, language) {
        viewModel.getAssistantGreeting()
    }

    val context = LocalContext.current
    val speechManager = remember { com.example.speech.SpeechManager.getInstance(context) }
    var recognizedText by remember { mutableStateOf("") }
    var speechErrorMsg by remember { mutableStateOf<String?>(null) }
    var showOfflineNoSupportDialog by remember { mutableStateOf(false) }
    
    var showOfflineDownloadPrompt by remember { mutableStateOf(false) }
    var showOfflineDownloadingProgress by remember { mutableStateOf(false) }
    var offlineDownloadProgress by remember { mutableStateOf(0) }
    var offlineDownloadErrorMsg by remember { mutableStateOf<String?>(null) }

    // Lifecycle observer to cancel listening when Activity/Screen pauses
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                speechManager.cancelListening()
                viewModel.stopVoiceRecording("")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            speechManager.cancelListening()
        }
    }

    fun startListening() {
        recognizedText = ""
        speechErrorMsg = null
        
        val isNetworkAvailable = viewModel.isNetworkAvailable()
        val isOfflineSupported = speechManager.isOfflineSupported(language)
        
        if (!isNetworkAvailable && !isOfflineSupported) {
            showOfflineDownloadPrompt = true
            return
        }
        
        speechManager.startListening(
            language = language,
            onStart = {
                viewModel.startVoiceRecording()
            },
            onPartialResult = { partialText ->
                recognizedText = partialText
            },
            onFinalResult = { finalText ->
                if (finalText.isNotEmpty()) {
                    viewModel.stopVoiceRecording(finalText)
                } else {
                    viewModel.stopVoiceRecording("")
                }
            },
            onError = { errorCode, originalMsg ->
                viewModel.stopVoiceRecording("") // Stop state
                val textToPass = recognizedText.ifEmpty { localVoiceText }
                if (textToPass.trim().isNotEmpty()) {
                    viewModel.setPartialVoiceInputText(textToPass)
                }
                // Automatically open manual task creation screen on speech recognition failure
                onNavigateToCreate()
            }
        )
    }

    fun stopListening() {
        speechManager.stopListening()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
        } else {
            speechErrorMsg = if (language == "ar") "يرجى تفعيل إذن الميكروفون لاستخدام التسجيل الصوتي." else "Microphone permission is required for voice input."
        }
    }

    // Infinite pulsing animation for Microphone
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        if (showOfflineDownloadPrompt) {
            AlertDialog(
                onDismissRequest = { showOfflineDownloadPrompt = false },
                title = {
                    Text(
                        text = if (language == "ar") "تحميل حزمة اللغة دون اتصال" else "Download Offline Language Pack",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = if (language == "ar") {
                            "أنت غير متصل بالإنترنت حالياً وحزمة اللغة العربية غير مثبتة على جهازك.\n\nهل ترغب في تحميل الحزمة للتعرف الصوتي دون اتصال؟"
                        } else {
                            "You are currently offline and the required speech language pack is not installed.\n\nWould you like to download the offline package now?"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOfflineDownloadPrompt = false
                            showOfflineDownloadingProgress = true
                            offlineDownloadProgress = 0
                            offlineDownloadErrorMsg = null
                            speechManager.triggerOfflineDownload(
                                language = language,
                                onProgress = { progress ->
                                    offlineDownloadProgress = progress
                                },
                                onSuccess = {
                                    showOfflineDownloadingProgress = false
                                    startListening()
                                },
                                onError = { errorMsg ->
                                    showOfflineDownloadingProgress = false
                                    offlineDownloadErrorMsg = errorMsg
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue)
                    ) {
                        Text(if (language == "ar") "تحميل الآن" else "Download Now", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showOfflineDownloadPrompt = false
                        viewModel.setPartialVoiceInputText("")
                        onNavigateToCreate()
                    }) {
                        Text(if (language == "ar") "اكتب يدوياً" else "Type Manually", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (showOfflineDownloadingProgress) {
            AlertDialog(
                onDismissRequest = { },
                title = {
                    Text(
                        text = if (language == "ar") "جاري تحميل حزمة اللغة..." else "Downloading Language Pack...",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        LinearProgressIndicator(
                            progress = { offlineDownloadProgress.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            color = PremiumBlue
                        )
                        Text(
                            text = "$offlineDownloadProgress%",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                confirmButton = {},
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        if (offlineDownloadErrorMsg != null) {
            AlertDialog(
                onDismissRequest = { offlineDownloadErrorMsg = null },
                title = {
                    Text(
                        text = if (language == "ar") "فشل التحميل" else "Download Failed",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = offlineDownloadErrorMsg ?: "",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { offlineDownloadErrorMsg = null },
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue)
                    ) {
                        Text(if (language == "ar") "موافق" else "OK", color = Color.White)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header: Dynamic Friendly Assistant Greeting
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val name = viewModel.nickname.collectAsState().value.ifEmpty { "Michael" }
                    val greetingTitle = if (language == "ar") {
                        "مرحباً يا $name 👋"
                    } else {
                        "Hello, $name 👋"
                    }
                    Text(
                        text = greetingTitle,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp
                        )
                    )


                }

                val promptText = if (language == "ar") {
                    "ما المهمة التي تريد مني أن أتذكرها لك؟"
                } else {
                    "What task would you like me to remember for you?"
                }
                Text(
                    text = promptText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = PremiumAccent,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // The glowing bubble assistantGreeting
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = assistantGreeting,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        ),
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
            }

            // Center: Big Microphone Identity
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Background Glow behind the mic
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(240.dp)
                ) {
                    // Radial background glow mimicking blur-[100px] opacity-10 in tailwind
                    Box(
                        modifier = Modifier
                            .size(220.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        PremiumBlue.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Pulsing Rings around Microphone
                    val pulseScaleInner = pulseScale * 0.9f
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(if (isRecording) pulseScale else 1.25f)
                            .border(
                                width = 1.dp,
                                color = PremiumAccent.copy(alpha = if (isRecording) 0.3f else 0.15f),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .scale(if (isRecording) pulseScaleInner else 1.50f)
                            .border(
                                width = 1.dp,
                                color = PremiumAccent.copy(alpha = if (isRecording) 0.15f else 0.08f),
                                shape = CircleShape
                            )
                    )

                    // Main Circular Mic Button
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = if (isRecording) listOf(DangerRed, Color(0xFF991B1B))
                                    else listOf(PremiumBlue, Color(0xFF1D4ED8))
                                )
                            )
                            .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .clickable {
                                if (isRecording) {
                                    stopListening()
                                } else {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    
                                    if (hasPermission) {
                                        startListening()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = "Microphone Trigger",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tap to speak
                Text(
                    text = if (isRecording) {
                        Localization.translate("voice_input_listening", language).uppercase()
                    } else {
                        if (language == "ar") "انقر للتحدث" else "TAP TO SPEAK"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = PremiumAccent,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier.padding(top = 12.dp),
                    textAlign = TextAlign.Center
                )

                if (speechErrorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = speechErrorMsg!!,
                        style = MaterialTheme.typography.bodySmall.copy(color = DangerRed),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Waveform or Instructions below Mic
                AnimatedVisibility(
                    visible = isRecording,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp)
                    ) {
                        // Display simple simulated voice waveform bar animation
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val waves = listOf(0.4f, 0.8f, 0.5f, 0.9f, 0.3f, 0.7f, 0.4f)
                            waves.forEachIndexed { i, heightFactor ->
                                val animatedHeight by infiniteTransition.animateFloat(
                                    initialValue = 10.dp.value,
                                    targetValue = (50.dp.value * heightFactor),
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(300 + i * 50, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "wave_$i"
                                )
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(animatedHeight.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(SuccessGreen)
                                )
                            }
                        }

                        if (recognizedText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "\"$recognizedText\"",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }

                // Practical emulator typing backup for robust testing
                if (!isRecording && voiceUiState is VoiceUiState.Idle) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = Localization.translate("voice_test_hint", language),
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = localVoiceText,
                                onValueChange = { localVoiceText = it },
                                placeholder = { Text("e.g. Call John tomorrow at 5 PM", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PremiumAccent,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )
                            Button(
                                onClick = {
                                    if (localVoiceText.trim().isNotEmpty()) {
                                        viewModel.stopVoiceRecording(localVoiceText)
                                        localVoiceText = ""
                                    }
                                },
                                enabled = localVoiceText.trim().isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(Localization.translate("submit_test", language))
                            }
                        }
                    }
                }

                // Bottom Navigation/Action button: "My Tasks"
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTasks() }
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Icon in a custom colored container
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(PremiumBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "My Tasks Icon",
                                    tint = PremiumBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Text Info
                            Column {
                                Text(
                                    text = Localization.translate("my_tasks", language),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (language == "ar") "إدارة تذكيراتك الذكية" else "Manage your smart reminders",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        // Right arrow circle icon
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Navigate to Tasks",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // Processing State Overlay
        if (voiceUiState is VoiceUiState.Processing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .width(300.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = PremiumAccent)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = Localization.translate("processing_voice", language),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Preview Task Dialog State
        if (voiceUiState is VoiceUiState.Preview) {
            val parsed = (voiceUiState as VoiceUiState.Preview).parsedTask
            ParsedTaskPreviewDialog(
                parsedTask = parsed,
                language = language,
                onDismiss = { viewModel.cancelVoicePreview() },
                onConfirm = { title, desc, date, time, priority, category, repeat ->
                    viewModel.saveTaskFromPreview(title, desc, date, time, priority, category, repeat)
                }
            )
        }

        // Error State Alert
        if (voiceUiState is VoiceUiState.Error) {
            val err = (voiceUiState as VoiceUiState.Error).message
            AlertDialog(
                onDismissRequest = { viewModel.cancelVoicePreview() },
                title = { Text("Error", color = MaterialTheme.colorScheme.onSurface) },
                text = { Text(err, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    TextButton(onClick = { viewModel.cancelVoicePreview() }) {
                        Text("OK", color = PremiumAccent)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParsedTaskPreviewDialog(
    parsedTask: ParsedTaskResult,
    language: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, TaskPriority, TaskCategory, TaskRepeat) -> Unit
) {
    var title by remember { mutableStateOf(parsedTask.title) }
    var description by remember { mutableStateOf(parsedTask.description) }
    var dueDate by remember { mutableStateOf(parsedTask.dueDate) }
    var dueTime by remember { mutableStateOf(parsedTask.dueTime) }
    
    var priority by remember {
        mutableStateOf(
            try { TaskPriority.valueOf(parsedTask.priority) } catch (e: Exception) { TaskPriority.NORMAL }
        )
    }
    var category by remember {
        mutableStateOf(
            try { TaskCategory.valueOf(parsedTask.category) } catch (e: Exception) { TaskCategory.OTHER }
        )
    }
    var repeat by remember {
        mutableStateOf(
            try { TaskRepeat.valueOf(parsedTask.repeat) } catch (e: Exception) { TaskRepeat.NONE }
        )
    }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Date Picker Dialog helper
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val mo = String.format("%02d", month + 1)
            val dy = String.format("%02d", dayOfMonth)
            dueDate = "$year-$mo-$dy"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Time Picker Dialog helper
    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val hr = String.format("%02d", hourOfDay)
            val min = String.format("%02d", minute)
            dueTime = "$hr:$min"
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = Localization.translate("preview_task", language),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Localization.translate("title", language), color = PremiumAccent) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = PremiumAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Localization.translate("description", language), color = PremiumAccent) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = PremiumAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Date and Time Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Localization.translate("date", language), color = PremiumAccent) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = PremiumAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = PremiumAccent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = Localization.formatTimeTo12Hour(dueTime, language),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(Localization.translate("time", language), color = PremiumAccent) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = PremiumAccent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        ),
                        trailingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = PremiumAccent)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Priority dropdown/selector
                Text(
                    text = Localization.translate("priority", language),
                    style = MaterialTheme.typography.bodySmall.copy(color = PremiumAccent)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    TaskPriority.values().forEach { prio ->
                        val selected = priority == prio
                        val color = when (prio) {
                            TaskPriority.CRITICAL -> DangerRed
                            TaskPriority.IMPORTANT -> WarningOrange
                            TaskPriority.NORMAL -> PremiumBlue
                            TaskPriority.CAN_WAIT -> SuccessGreen
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) color else MaterialTheme.colorScheme.surface)
                                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { priority = prio }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Localization.translate(prio.name.lowercase(), language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Category dropdown/selector
                Text(
                    text = Localization.translate("category", language),
                    style = MaterialTheme.typography.bodySmall.copy(color = PremiumAccent)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    listOf(TaskCategory.PERSONAL, TaskCategory.WORK, TaskCategory.HEALTH, TaskCategory.SHOPPING, TaskCategory.OTHER).forEach { cat ->
                        val selected = category == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) PremiumBlue else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Localization.translate(cat.name.lowercase(), language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Repeat Selection
                Text(
                    text = Localization.translate("repeat", language),
                    style = MaterialTheme.typography.bodySmall.copy(color = PremiumAccent)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    TaskRepeat.values().forEach { rep ->
                        val selected = repeat == rep
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) PremiumBlue else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .clickable { repeat = rep }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Localization.translate(rep.name.lowercase(), language),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, description, dueDate, dueTime, priority, category, repeat) },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue)
            ) {
                Text(Localization.translate("save", language), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.translate("cancel", language), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.padding(vertical = 24.dp)
    )
}
