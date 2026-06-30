package com.example.presentation.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.SopViewModel
import com.example.domain.model.TaskRepeat
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StatisticsScreen(viewModel: SopViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val language by viewModel.language.collectAsState()

    // Calculations
    val totalTasksCount = tasks.size
    val completedTasksCount = tasks.filter { it.isCompleted }.size
    val remainingTasksCount = totalTasksCount - completedTasksCount
    
    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    val todayTasks = tasks.filter { it.dueDate == todayStr }
    val todayCompletedCount = todayTasks.filter { it.isCompleted }.size
    val todayTotalCount = todayTasks.size

    val completionRate = if (totalTasksCount > 0) {
        (completedTasksCount.toFloat() / totalTasksCount.toFloat())
    } else {
        0f
    }

    // Animation for circular dial
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(completionRate) {
        animatedProgress.animateTo(
            targetValue = completionRate,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    // Weekly statistics calculations (last 7 days)
    val weeklyStats = remember(tasks) {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dayFormat = SimpleDateFormat("EEE", Locale.US)
        
        val list = mutableListOf<DayStat>()
        // Generate stats for past 7 days
        for (i in 6 downTo 0) {
            val dateCal = Calendar.getInstance()
            dateCal.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(dateCal.time)
            val dayName = dayFormat.format(dateCal.time)
            
            val dayTasks = tasks.filter { it.dueDate == dateStr }
            val dayCompleted = dayTasks.filter { it.isCompleted }.size
            val dayTotal = dayTasks.size
            val rate = if (dayTotal > 0) dayCompleted.toFloat() / dayTotal.toFloat() else 0f
            
            list.add(DayStat(dayName, rate))
        }
        list
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Title
        Text(
            text = Localization.translate("statistics", language),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )

        // Main Animated Circular Gauge Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = Localization.translate("completion_rate", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(180.dp)
                ) {
                    // Resolve colors outside Canvas drawing scope
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    
                    // Canvas Circle drawing
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = (size.width - strokeWidth) / 2

                        // Underlay track
                        drawCircle(
                            color = trackColor,
                            radius = radius,
                            center = center,
                            style = Stroke(width = strokeWidth)
                        )

                        // Highlight completed arc
                        drawArc(
                            color = primaryColor,
                            startAngle = -90f,
                            sweepAngle = animatedProgress.value * 360f,
                            useCenter = false,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = Size(size.width - strokeWidth, size.height - strokeWidth),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // Numeric percentage overlay inside the dial
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(completionRate * 100).roundToInt()}%",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Black,
                                fontSize = 38.sp
                            )
                        )
                        Text(
                            text = "${completedTasksCount}/${totalTasksCount}",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )
                    }
                }
            }
        }

        // Summary Statistics Grid Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatMetricCard(
                title = Localization.translate("completed_tasks", language),
                value = completedTasksCount.toString(),
                icon = Icons.Default.CheckCircle,
                iconColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )

            StatMetricCard(
                title = Localization.translate("remaining_tasks", language),
                value = remainingTasksCount.toString(),
                icon = Icons.Default.PendingActions,
                iconColor = WarningOrange,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            StatMetricCard(
                title = Localization.translate("todays_tasks", language),
                value = "${todayCompletedCount}/${todayTotalCount}",
                icon = Icons.Default.Today,
                iconColor = PremiumAccent,
                modifier = Modifier.weight(1f)
            )

            StatMetricCard(
                title = Localization.translate("repeat", language),
                value = tasks.filter { it.repeat != TaskRepeat.NONE }.size.toString(),
                icon = Icons.Default.Autorenew,
                iconColor = SuccessGreen,
                modifier = Modifier.weight(1f)
            )
        }

        // Weekly Progress Chart
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = Localization.translate("weekly_progress", language),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                )

                // Weekly Chart Canvas
                val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                val barActiveColor = MaterialTheme.colorScheme.secondary
                val barInactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val chartHeight = size.height
                        val chartWidth = size.width
                        val padding = 16.dp.toPx()
                        
                        val activeWidth = chartWidth - padding * 2
                        val colWidth = 24.dp.toPx()
                        val colSpacing = (activeWidth - (colWidth * weeklyStats.size)) / (weeklyStats.size - 1)

                        // Draw Grid Horizontal Lines
                        val lineCount = 4
                        for (i in 0 until lineCount) {
                            val y = chartHeight * (i.toFloat() / (lineCount - 1))
                            drawLine(
                                color = gridColor,
                                start = Offset(padding, y),
                                end = Offset(chartWidth - padding, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Draw Columns
                        weeklyStats.forEachIndexed { index, stat ->
                            val x = padding + index * (colWidth + colSpacing)
                            val barHeight = chartHeight * stat.rate
                            val y = chartHeight - barHeight

                            // Draw Rounded Column
                            drawRoundRect(
                                color = if (stat.rate > 0) barActiveColor else barInactiveColor,
                                topLeft = Offset(x, y),
                                size = Size(colWidth, barHeight.coerceAtLeast(8.dp.toPx())),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                            )
                        }
                    }
                }

                // Columns Labels Row
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    weeklyStats.forEach { stat ->
                        Text(
                            text = stat.dayName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.width(32.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

data class DayStat(val dayName: String, val rate: Float)

@Composable
fun StatMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

fun Float.roundToInt(): Int = kotlin.math.round(this).toInt()
