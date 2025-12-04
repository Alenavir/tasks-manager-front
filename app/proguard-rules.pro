# Сохраняем все DTO для Retrofit и Gson
-keep class ru.alenavir.tasks.data.dto.** { *; }

# Сохраняем все enum’ы
-keepclassmembers enum * { *; }

# Сохраняем аннотации Kotlin
-keepclassmembers class kotlin.Metadata { *; }

# Retrofit & OkHttp
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
