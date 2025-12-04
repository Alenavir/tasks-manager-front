package ru.alenavir.tasks.data.api

import retrofit2.Response
import retrofit2.http.*
import ru.alenavir.tasks.data.dto.UserDto
import ru.alenavir.tasks.data.dto.enums.Status

interface UserApi {

    @GET("/api/users/user")
    suspend fun getById(): Response<UserDto>
    @PUT("/api/users/user")
    suspend fun update(
        @Body dto: UserDto
    ): Response<UserDto>

}
