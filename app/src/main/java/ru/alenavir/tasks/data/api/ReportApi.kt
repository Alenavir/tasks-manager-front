package ru.alenavir.tasks.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import ru.alenavir.tasks.data.dto.CategoryStatsDto
import ru.alenavir.tasks.data.dto.enums.Status

interface ReportApi {

    // Получить количество задач по категориям за месяц
    @GET("/api/report/statistics/categories")
    suspend fun getTaskCountByCategoryForMonth(
        @Query("month") month: String // формат "yyyy-MM"
    ): Response<List<CategoryStatsDto>>

    // Получить количество задач по статусу за месяц
    @GET("/api/report/statistics/status")
    suspend fun getTaskCountByStatusForMonth(
        @Query("month") month: String // формат "yyyy-MM"
    ): Response<Map<Status, Long>>
}
