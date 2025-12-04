package ru.alenavir.tasks.data.api

import retrofit2.Response
import retrofit2.http.*
import ru.alenavir.tasks.data.dto.TaskDto
import ru.alenavir.tasks.data.dto.enums.Priority

interface TaskApi {

    @POST("/api/tasks")
    suspend fun createTask(@Body dto: TaskDto): Response<TaskDto>

    @GET("/api/tasks/{id}")
    suspend fun getTaskById(@Path("id") id: Int): Response<TaskDto>

    @GET("/api/tasks/status-done")
    suspend fun getTasksDone(): Response<List<TaskDto>>

    @GET("/api/tasks/status-todo")
    suspend fun getTasksToDo(): Response<List<TaskDto>>

    @PUT("/api/tasks/{id}")
    suspend fun updateTask(@Path("id") id: Int, @Body dto: TaskDto): Response<TaskDto>

    @DELETE("/api/tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int): Response<Unit>

    @GET("/api/tasks/date")
    suspend fun getTasksByDate(
        @Query("date") date: String,
        @Query("categoryId") categoryId: Int? = null,
        @Query("isPriority") isPriority: Boolean? = null
    ): Response<List<TaskDto>>

    @PUT("/api/tasks/status-done/{id}")
    suspend fun setTaskDone(@Path("id") id: Int): Response<Boolean>

    @PUT("/api/tasks/status-todo/{id}")
    suspend fun setTaskToDo(@Path("id") id: Int): Response<Boolean>

    @GET("/api/tasks/filter/{categoryId}")
    suspend fun getTasksFiltered(
        @Path("categoryId") categoryId: Int,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): Response<List<TaskDto>>
}
