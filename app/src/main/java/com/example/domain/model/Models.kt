package com.example.domain.model

enum class TaskPriority {
    CRITICAL,
    IMPORTANT,
    NORMAL,
    CAN_WAIT
}

enum class TaskCategory {
    PERSONAL,
    WORK,
    HEALTH,
    SHOPPING,
    OTHER
}

enum class TaskRepeat {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY
}

data class Task(
    val id: String,
    val title: String,
    val description: String,
    val dueDate: String, // YYYY-MM-DD
    val dueTime: String, // HH:MM
    val priority: TaskPriority,
    val category: TaskCategory,
    val repeat: TaskRepeat,
    val isCompleted: Boolean,
    val syncPending: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
    val userId: String = ""
)
