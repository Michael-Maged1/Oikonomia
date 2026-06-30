package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.Task
import com.example.domain.model.TaskCategory
import com.example.domain.model.TaskPriority
import com.example.domain.model.TaskRepeat

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val dueDate: String,
    val dueTime: String,
    val priority: String, // Stored as String enum name
    val category: String, // Stored as String enum name
    val repeat: String,   // Stored as String enum name
    val isCompleted: Boolean,
    val syncPending: Boolean,
    val lastUpdated: Long,
    val userId: String
)

fun TaskEntity.toDomain(): Task {
    return Task(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        dueTime = dueTime,
        priority = try { TaskPriority.valueOf(priority) } catch (e: Exception) { TaskPriority.NORMAL },
        category = try { TaskCategory.valueOf(category) } catch (e: Exception) { TaskCategory.OTHER },
        repeat = try { TaskRepeat.valueOf(repeat) } catch (e: Exception) { TaskRepeat.NONE },
        isCompleted = isCompleted,
        syncPending = syncPending,
        lastUpdated = lastUpdated,
        userId = userId
    )
}

fun Task.toEntity(): TaskEntity {
    return TaskEntity(
        id = id,
        title = title,
        description = description,
        dueDate = dueDate,
        dueTime = dueTime,
        priority = priority.name,
        category = category.name,
        repeat = repeat.name,
        isCompleted = isCompleted,
        syncPending = syncPending,
        lastUpdated = lastUpdated,
        userId = userId
    )
}
