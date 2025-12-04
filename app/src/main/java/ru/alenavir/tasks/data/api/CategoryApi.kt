package ru.alenavir.tasks.data.api

import retrofit2.Response
import retrofit2.http.*
import ru.alenavir.tasks.data.dto.CategoryDto

interface CategoryApi {

    @POST("/api/categories")
    suspend fun createCategory(@Body dto: CategoryDto): Response<CategoryDto>

    @GET("/api/categories/{id}")
    suspend fun getCategoryById(@Path("id") id: Int): Response<CategoryDto>

    @PUT("/api/categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body dto: CategoryDto): Response<CategoryDto>

    @DELETE("/api/categories/{id}")
    suspend fun deleteCategoryById(@Path("id") id: Int): Response<Void>

    @GET("/api/categories")
    suspend fun getAllCategories(): Response<List<CategoryDto>>
}
