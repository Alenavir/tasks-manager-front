package ru.alenavir.tasks.data.api

import android.content.Context
import com.google.gson.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ru.alenavir.tasks.data.network.TokenAuthenticator
import ru.alenavir.tasks.data.network.TokenInterceptor
import java.lang.reflect.Type
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object RetrofitClient {

    const val BASE_URL = "http://10.0.2.2:8080"

    private fun createGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(
                LocalDateTime::class.java,
                object : JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
                    override fun serialize(
                        src: LocalDateTime?,
                        typeOfSrc: Type?,
                        context: JsonSerializationContext?
                    ): JsonElement {
                        return JsonPrimitive(src?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    }

                    override fun deserialize(
                        json: JsonElement?,
                        typeOfT: Type?,
                        context: JsonDeserializationContext?
                    ): LocalDateTime {
                        return LocalDateTime.parse(json?.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    }
                }
            )
            .create()
    }

    private fun createClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(context))     // добавляет токен
            .authenticator(TokenAuthenticator(context))    // обновляет токен
            .build()
    }

    private var retrofit: Retrofit? = null

    private fun getRetrofit(context: Context): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create(createGson()))
                .client(createClient(context))
                .build()
        }
        return retrofit!!
    }

    fun createAuthApi(context: Context) = getRetrofit(context).create(AuthApi::class.java)

    fun createTaskApi(context: Context) = getRetrofit(context).create(TaskApi::class.java)

    fun createCategoryApi(context: Context) = getRetrofit(context).create(CategoryApi::class.java)

    fun createUserApi(context: Context) = getRetrofit(context).create(UserApi::class.java)

    fun createReportApi(context: Context) = getRetrofit(context).create(ReportApi::class.java)
}
