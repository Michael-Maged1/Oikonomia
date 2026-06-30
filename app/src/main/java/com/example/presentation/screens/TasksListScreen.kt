package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Task
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRepeat
import com.example.presentation.viewmodel.SopViewModel
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksListScreen(
    viewModel: SopViewModel,
    onNavigateToCreate: () -> Unit,
    onEditTask: (Task) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val language by viewModel.language.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<TaskCategory?>(null) }
    var selectedPriorityFilter by remember { mutableStateOf<TaskPriority?>(null) }

    val filteredTasks = remember(tasks, searchQuery, selectedCategoryFilter, selectedPriorityFilter) {
        tasks.filter { task ->
            val matchesSearch = task.title.contains(searchQuery, ignoreCase = true) ||
                    task.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryFilter == null || task.category == selectedCategoryFilter
            val matchesPriority = selectedPriorityFilter == null || task.priority == selectedPriorityFilter
            matchesSearch && matchesCategory && matchesPriority
        }
    }

    val nickname by viewModel.nickname.collectAsState()
    val todayTasksCount = remember(tasks) {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        tasks.filter { it.dueDate == todayStr }.size
    }
    
    val formattedDate = remember(language) {
        val locale = java.util.Locale(language)
        val dateFormat = java.text.SimpleDateFormat("EEEE، d MMMM", locale)
        dateFormat.format(java.util.Date())
    }
    
    val greetingText = remember(nickname, language) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val name = nickname.ifEmpty { "Michael" }
        if (language == "ar") {
            when (hour) {
                in 5..11 -> "صباح الخير يا $name ☀️"
                in 12..16 -> "مساء الخير يا $name 👋"
                in 17..20 -> "مساء الخير يا $name 🌟"
                else -> "مساء هادئ يا $name 🌙"
            }
        } else {
            when (hour) {
                in 5..11 -> "Good morning, $name ☀️"
                in 12..16 -> "Good afternoon, $name 👋"
                in 17..20 -> "Good evening, $name 🌟"
                else -> "Hello, $name ❤️"
            }
        }
    }

    val taskCountText = remember(todayTasksCount, language) {
        if (language == "ar") {
            val easternCount = viewModel.formatToArabicDigits(todayTasksCount)
            "اليوم لديك $easternCount مهام."
        } else {
            "Today you have $todayTasksCount tasks."
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = PremiumBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Premium Home Header Section
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = greetingText,
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PremiumBlue.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = PremiumAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Text(
                        text = taskCountText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(Localization.translate("search_placeholder", language), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PremiumAccent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips Row
            Text(
                text = Localization.translate("category", language),
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp)
            ) {
                // "All" chip
                FilterChip(
                    selected = selectedCategoryFilter == null,
                    onClick = { selectedCategoryFilter = null },
                    label = { Text(Localization.translate("all", language)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PremiumBlue,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                TaskCategory.values().forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryFilter == cat,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text(Localization.translate(cat.name.lowercase(), language)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PremiumBlue,
                            selectedLabelColor = Color.White,
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // Task List
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        // Premium Illustration Glow Box
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(140.dp)
                        ) {
                            // Subtle radial background gradient
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                PremiumBlue.copy(alpha = 0.12f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            // Glowing Ring
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .border(
                                        width = 1.5.dp,
                                        color = PremiumAccent.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                            )
                            // Smaller Glowing Solid Ring
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .background(
                                        color = PremiumBlue.copy(alpha = 0.08f),
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = PremiumAccent.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "All Done",
                                    tint = PremiumAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = if (language == "ar") "قائمتك خالية من المهام ✨" else "Your list is perfectly clean ✨",
                            color = MaterialTheme.colorScheme.onBackground,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            ),
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = if (language == "ar") {
                                "ابدأ بإنشاء تذكير جديد ومهمة جديدة بصوتك الآن، وسأقوم بتنظيمها وتذكيرك بها!"
                            } else {
                                "Create a new reminder with your voice, and I will organize and remind you about it!"
                            },
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        SwipeableTaskCard(
                            task = task,
                            language = language,
                            onToggleComplete = { viewModel.toggleTaskCompleted(task) },
                            onDelete = { viewModel.deleteTask(task) },
                            onLongPress = { onEditTask(task) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableTaskCard(
    task: Task,
    language: String,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onLongPress: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }

    val priorityColor = when (task.priority) {
        TaskPriority.CRITICAL -> DangerRed
        TaskPriority.IMPORTANT -> WarningOrange
        TaskPriority.NORMAL -> PremiumBlue
        TaskPriority.CAN_WAIT -> SuccessGreen
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (task.isCompleted) SuccessGreen.copy(alpha = 0.3f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                RoundedCornerShape(16.dp)
            )
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (offsetX < -120f) {
                            // Swipe Left -> Delete
                            onDelete()
                        } else if (offsetX > 120f) {
                            // Swipe Right -> Toggle Complete
                            onToggleComplete()
                        }
                        offsetX = 0f
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount
                    }
                )
            }
    ) {
        // Background swipe hints
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Swipe Right: Complete icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Swipe Complete",
                tint = SuccessGreen.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )

            // Swipe Left: Delete icon
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Swipe Delete",
                tint = DangerRed.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }

        // Main Foreground Card
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(
                    onLongClick = onLongPress,
                    onClick = onToggleComplete
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Priority color indicator bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(priorityColor)
                )

                // Task Information
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Sync Status Indicator
                        if (task.syncPending) {
                            Icon(
                                imageVector = Icons.Default.CloudQueue,
                                contentDescription = "Offline Pending Sync",
                                tint = WarningOrange,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (task.description.isNotEmpty()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    // Date & Time Row
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Due Date Time",
                            tint = PremiumAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${task.dueDate}  |  ${Localization.formatTimeTo12Hour(task.dueTime, language)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                // Completion status check icon
                IconButton(onClick = onToggleComplete) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Complete",
                        tint = if (task.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
