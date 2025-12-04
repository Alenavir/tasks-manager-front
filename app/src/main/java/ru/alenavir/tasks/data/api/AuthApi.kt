package ru.alenavir.tasks.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import ru.alenavir.tasks.data.dto.JwtAuthenticationDto
import ru.alenavir.tasks.data.dto.RefreshTokenDto
import ru.alenavir.tasks.data.dto.RegistrationDto
import ru.alenavir.tasks.data.dto.UserCredentialsDto

interface AuthApi {

    @POST("/auth/sign-in")
    suspend fun signIn(@Body body: UserCredentialsDto): Response<JwtAuthenticationDto>

    @POST("/auth/refresh")
    suspend fun refresh(@Body body: RefreshTokenDto): Response<JwtAuthenticationDto>

    @POST("/api/users/registration")
    suspend fun register(@Body body: RegistrationDto): Response<Unit>

}