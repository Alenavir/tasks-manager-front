package ru.alenavir.tasks.data.dto

data class JwtAuthenticationDto(
    val accessToken: String,
    val refreshToken: String
)