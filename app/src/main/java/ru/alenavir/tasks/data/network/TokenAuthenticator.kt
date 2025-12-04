package ru.alenavir.tasks.data.network

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import ru.alenavir.tasks.data.api.RetrofitClient
import ru.alenavir.tasks.data.dto.RefreshTokenDto
import ru.alenavir.tasks.ui.login.LoginActivity

class TokenAuthenticator(private val context: Context) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.e("TokenAuthenticator", "401 пойман, пробую обновить токен")

        // Чтобы не зациклиться
        if (responseCount(response) >= 2) {
            Log.e("TokenAuthenticator", "Слишком много попыток — выходим в логин")
            logout()
            return null
        }

        val refresh = runBlocking { TokenStorage.getRefresh(context) }

        if (refresh.isNullOrEmpty()) {
            Log.e("TokenAuthenticator", "Refresh токена нет — logout")
            logout()
            return null
        }

        // Вызываем refresh
        val refreshResponse = runBlocking {
            RetrofitClient.createAuthApi(context).refresh(RefreshTokenDto(refresh))
        }

        if (!refreshResponse.isSuccessful) {
            Log.e("TokenAuthenticator", "Refresh протух — logout")
            logout()
            return null
        }

        val tokens = refreshResponse.body()!!
        val newAccess = tokens.accessToken
        val newRefresh = tokens.refreshToken

        // Сохраняем новые токены
        runBlocking {
            TokenStorage.saveTokens(context, newAccess, newRefresh)
        }

        Log.d("TokenAuthenticator", "Токены успешно обновлены")

        // Повторяем исходный запрос с новым токеном
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }

    private fun logout() {
        runBlocking {
            TokenStorage.clear(context)
        }

        val intent = Intent(context, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }


    private fun responseCount(response: Response): Int {
        var count = 1
        var r: Response? = response.priorResponse
        while (r != null) {
            count++
            r = r.priorResponse
        }
        return count
    }
}
