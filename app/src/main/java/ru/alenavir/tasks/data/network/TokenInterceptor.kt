package ru.alenavir.tasks.data.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class TokenInterceptor(private val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        val token = runBlocking {
            TokenStorage.getAccess(context)
        }

        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        if (token != null) {
            Log.d("TokenInterceptor", "Добавляю токен к запросу: ${originalRequest.method} ${originalRequest.url}")
            requestBuilder.addHeader("Authorization", "Bearer $token")
        } else {
            Log.e("TokenInterceptor", "Токена НЕТ! Для запроса: ${originalRequest.method} ${originalRequest.url}")
        }

        return chain.proceed(requestBuilder.build())
    }
}

