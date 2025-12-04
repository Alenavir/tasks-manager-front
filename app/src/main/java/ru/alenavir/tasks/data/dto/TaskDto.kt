package ru.alenavir.tasks.data.dto

import ru.alenavir.tasks.data.dto.enums.Priority
import ru.alenavir.tasks.data.dto.enums.Status
import java.time.LocalDate
import java.time.LocalDateTime

data class TaskDto(
    val id: Int? = null,
    val title: String,
    val description: String,
    val priority: Priority,
    var status: Status,
    val categoryId: Int?,
    val date: String
)
