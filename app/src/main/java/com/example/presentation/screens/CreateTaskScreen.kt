package com.example.presentation.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Task
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRepeat
import com.example.presentation.viewmodel.SopViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskScreen(
    viewModel: SopViewModel,
    taskId: String?,
    onBack: () -> Unit
) {
    val language by viewModel.language.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dueDate by remember { mutableStateOf("") }
    var dueTime by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(TaskPriority.NORMAL) }
    var category by remember { mutableStateOf(TaskCategory.OTHER) }
    var repeat by remember { mutableStateOf(TaskRepeat.NONE) }

    // State for Voice Input popup inside this screen
    var showVoicePopup by remember { mutableStateOf(false) }
    var voiceInputText by remember { mutableStateOf("") }
    var isAutofilling by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val partialText by viewModel.partialVoiceInputText.collectAsState()

    // Load existing task if editing or pre-fill with partial voice fallback
    LaunchedEffect(taskId, tasks, partialText) {
        if (taskId != null) {
            val task = tasks.find { it.id == taskId }
            if (task != null) {
                title = task.title
                description = task.description
                dueDate = task.dueDate
                dueTime = task.dueTime
                priority = task.priority
                category = task.category
                repeat = task.repeat
            }
        } else {
            // Set default date as today
            if (dueDate.isEmpty()) {
                dueDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            }
            if (partialText.isNotEmpty()) {
                title = partialText
                viewModel.clearPartialVoiceInputText()
            }
        }
    }

    // Date Picker Dialog
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

    // Time Picker Dialog
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (taskId != null) Localization.translate("update_task_manual", language)
                        else Localization.translate("create_task_manual", language),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    // Smart Voice input button
                    IconButton(onClick = { showVoicePopup = true }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice Input", tint = PremiumAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(Localization.translate("title", language), color = PremiumAccent) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = PremiumAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(Localization.translate("description", language), color = PremiumAccent) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = PremiumAccent,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                // Date & Time Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = PremiumAccent)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
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
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        trailingIcon = {
                            IconButton(onClick = { timePickerDialog.show() }) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = PremiumAccent)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Priority Selector
                Text(
                    text = Localization.translate("priority", language),
                    style = MaterialTheme.typography.bodySmall.copy(color = PremiumAccent, fontWeight = FontWeight.Bold)
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) color else MaterialTheme.colorScheme.surface)
                                .border(
                                    1.dp,
                                    if (selected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { priority = prio }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Localization.translate(prio.name.lowercase(), language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                // Category Selector
                Text(
                    text = Localization.translate("category", language),
                    style = MaterialTheme.typography.bodySmall.copy(color = PremiumAccent, fontWeight = FontWeight.Bold)
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) PremiumBlue else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .clickable { category = cat }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Localization.translate(cat.name.lowercase(), language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                // Repeat Selector
                Text(
                    text = Localization.translate("repeat", language),
                    style = MaterialTheme.typography.bodySmall.copy(color = PremiumAccent, fontWeight = FontWeight.Bold)
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
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) PremiumBlue else MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .clickable { repeat = rep }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = Localization.translate(rep.name.lowercase(), language),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Action
                Button(
                    onClick = {
                        if (title.trim().isNotEmpty()) {
                            viewModel.saveTaskManual(
                                id = taskId,
                                title = title,
                                description = description,
                                dueDate = dueDate,
                                dueTime = dueTime,
                                priority = priority,
                                category = category,
                                repeat = repeat
                            )
                            onBack()
                        }
                    },
                    enabled = title.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumBlue,
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = Localization.translate("save", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Voice Autofill Dialogue Overlay
            if (showVoicePopup) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = Localization.translate("voice_autofill", language),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            if (isAutofilling) {
                                CircularProgressIndicator(color = PremiumAccent)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = Localization.translate("processing_voice", language),
                                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            } else {
                                Text(
                                    text = Localization.translate("voice_input_speak", language),
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    textAlign = TextAlign.Center
                                )

                                OutlinedTextField(
                                    value = voiceInputText,
                                    onValueChange = { voiceInputText = it },
                                    placeholder = { Text("e.g. Call dentist tomorrow at 4 PM", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = PremiumAccent,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(
                                        onClick = {
                                            showVoicePopup = false
                                            voiceInputText = ""
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(Localization.translate("cancel", language), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Button(
                                        onClick = {
                                            if (voiceInputText.trim().isNotEmpty()) {
                                                isAutofilling = true
                                                viewModel.autofillTaskWithAi(voiceInputText) { result ->
                                                    title = result.title
                                                    description = result.description
                                                    if (result.dueDate.isNotEmpty()) dueDate = result.dueDate
                                                    if (result.dueTime.isNotEmpty()) dueTime = result.dueTime
                                                    priority = try { TaskPriority.valueOf(result.priority) } catch (e: Exception) { TaskPriority.NORMAL }
                                                    category = try { TaskCategory.valueOf(result.category) } catch (e: Exception) { TaskCategory.OTHER }
                                                    repeat = try { TaskRepeat.valueOf(result.repeat) } catch (e: Exception) { TaskRepeat.NONE }
                                                    
                                                    isAutofilling = false
                                                    showVoicePopup = false
                                                    voiceInputText = ""
                                                }
                                            }
                                        },
                                        enabled = voiceInputText.trim().isNotEmpty(),
                                        colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(Localization.translate("submit_test", language))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
