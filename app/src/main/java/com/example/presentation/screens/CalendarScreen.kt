package com.example.presentation.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Task
import com.example.domain.model.TaskPriority
import com.example.presentation.viewmodel.SopViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: SopViewModel,
    onEditTask: (Task) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val language by viewModel.language.collectAsState()

    var currentMonthCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDateStr by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }

    val monthYearFormat = remember(language) {
        if (language == "ar") SimpleDateFormat("MMMM yyyy", Locale("ar"))
        else SimpleDateFormat("MMMM yyyy", Locale.US)
    }

    // Days in current selected month
    val daysInMonth = remember(currentMonthCalendar) {
        val cal = currentMonthCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sunday = 0, Monday = 1)
        Pair(maxDays, firstDayOfWeek)
    }

    val (maxDays, firstDayOfWeek) = daysInMonth

    // Header Month controllers
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Title
        Text(
            text = Localization.translate("calendar", language),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Monthly Header Panel
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = {
                        val newCal = currentMonthCalendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, -1)
                        currentMonthCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Month", tint = MaterialTheme.colorScheme.onSurface)
                    }

                    Text(
                        text = monthYearFormat.format(currentMonthCalendar.time),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        ),
                        textAlign = TextAlign.Center
                    )

                    IconButton(onClick = {
                        val newCal = currentMonthCalendar.clone() as Calendar
                        newCal.add(Calendar.MONTH, 1)
                        currentMonthCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Month", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekday names
                val weekdays = listOf("S", "M", "T", "W", "T", "F", "S")
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekdays.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid Days
                val totalGridCells = 42 // 6 rows of 7 days
                val year = currentMonthCalendar.get(Calendar.YEAR)
                val month = currentMonthCalendar.get(Calendar.MONTH) // 0-indexed

                var cellCount = 0
                Column {
                    for (row in 0 until 6) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (col in 0 until 7) {
                                val dayNumber = cellCount - firstDayOfWeek + 1
                                val isValidDay = dayNumber in 1..maxDays

                                val dateString = if (isValidDay) {
                                    val formattedMo = String.format("%02d", month + 1)
                                    val formattedDy = String.format("%02d", dayNumber)
                                    "$year-$formattedMo-$formattedDy"
                                } else ""

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selectedDateStr == dateString && isValidDay) PremiumBlue
                                            else Color.Transparent
                                        )
                                        .clickable(enabled = isValidDay) {
                                            selectedDateStr = dateString
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isValidDay) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNumber.toString(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = if (selectedDateStr == dateString) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (selectedDateStr == dateString) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            )

                                            // Priority Dots for day tasks
                                            val dayTasks = tasks.filter { it.dueDate == dateString }
                                            if (dayTasks.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    val dotsToShow = dayTasks.take(3)
                                                    dotsToShow.forEach { t ->
                                                        val dotColor = when (t.priority) {
                                                            TaskPriority.CRITICAL -> DangerRed
                                                            TaskPriority.IMPORTANT -> WarningOrange
                                                            TaskPriority.NORMAL -> PremiumAccent
                                                            TaskPriority.CAN_WAIT -> SuccessGreen
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(dotColor)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                cellCount++
                            }
                        }
                    }
                }
            }
        }

        // Selected Day Task List Panel
        Text(
            text = selectedDateStr,
            style = MaterialTheme.typography.titleMedium.copy(
                color = PremiumAccent,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(top = 8.dp)
        )

        val selectedDayTasks = remember(tasks, selectedDateStr) {
            tasks.filter { it.dueDate == selectedDateStr }
        }

        if (selectedDayTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.EventNote, contentDescription = "No tasks", tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(Localization.translate("no_tasks", language), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(selectedDayTasks) { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEditTask(task) }
                            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val priorityColor = when (task.priority) {
                                TaskPriority.CRITICAL -> DangerRed
                                TaskPriority.IMPORTANT -> WarningOrange
                                TaskPriority.NORMAL -> PremiumBlue
                                TaskPriority.CAN_WAIT -> SuccessGreen
                            }

                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(36.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "${Localization.formatTimeTo12Hour(task.dueTime, language)}  |  ${Localization.translate(task.category.name.lowercase(), language)}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                )
                            }

                            if (task.isCompleted) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = SuccessGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}
