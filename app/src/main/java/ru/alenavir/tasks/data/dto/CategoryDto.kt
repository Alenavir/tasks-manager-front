package ru.alenavir.tasks.data.dto

import ru.alenavir.tasks.data.dto.enums.Priority
import java.time.LocalDateTime

data class CategoryDto(
    val id: Int? = null,
    val name: String,
    val color: String,
    val priority: Priority,
    val createdAt: LocalDateTime? = null
)
