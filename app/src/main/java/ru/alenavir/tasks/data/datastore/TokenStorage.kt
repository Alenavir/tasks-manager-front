import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey

import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

val Context.dataStore by preferencesDataStore("auth")

object TokenStorage {

    private var cachedAccess: String? = null
    private var cachedRefresh: String? = null

    private val ACCESS = stringPreferencesKey("access")
    private val REFRESH = stringPreferencesKey("refresh")

    suspend fun saveTokens(ctx: Context, access: String, refresh: String) {
        cachedAccess = access
        cachedRefresh = refresh
        ctx.dataStore.edit {
            it[ACCESS] = access
            it[REFRESH] = refresh
        }
    }

    suspend fun getAccess(ctx: Context): String? {
        if (cachedAccess != null) return cachedAccess
        cachedAccess = ctx.dataStore.data.first()[ACCESS]
        return cachedAccess
    }

    suspend fun getRefresh(ctx: Context): String? {
        if (cachedRefresh != null) return cachedRefresh
        cachedRefresh = ctx.dataStore.data.first()[REFRESH]
        return cachedRefresh
    }

    suspend fun clear(ctx: Context) {
        cachedAccess = null
        cachedRefresh = null
        ctx.dataStore.edit { it.clear() }
    }
}
