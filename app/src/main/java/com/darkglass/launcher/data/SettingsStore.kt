package com.darkglass.launcher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.darkglass.launcher.model.Favorites
import com.darkglass.launcher.model.Mappings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "darkglass")

data class UserSettings(
    val wallpaper: Int = 0,
    /** 0 = suave, 1 = médio, 2 = intenso */
    val glass: Int = 1,
    val animations: Boolean = true,
    val order: List<String> = Favorites.defaultOrder(),
    /** slotId → pacote associado manualmente */
    val manual: Map<String, String> = emptyMap(),
)

class SettingsStore(private val context: Context) {

    private object Keys {
        val WALLPAPER = intPreferencesKey("wallpaper")
        val GLASS = intPreferencesKey("glass")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val ORDER = stringPreferencesKey("order")
        val MANUAL = stringPreferencesKey("manual")
    }

    val settings: Flow<UserSettings> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { read(it) }

    private fun read(p: Preferences) = UserSettings(
        wallpaper = p[Keys.WALLPAPER] ?: 0,
        glass = (p[Keys.GLASS] ?: 1).coerceIn(0, 2),
        animations = p[Keys.ANIMATIONS] ?: true,
        order = Favorites.parseOrder(p[Keys.ORDER]),
        manual = Mappings.parse(p[Keys.MANUAL]),
    )

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.dataStore.edit { p ->
            val next = transform(read(p))
            p[Keys.WALLPAPER] = next.wallpaper
            p[Keys.GLASS] = next.glass
            p[Keys.ANIMATIONS] = next.animations
            p[Keys.ORDER] = Favorites.serializeOrder(next.order)
            p[Keys.MANUAL] = Mappings.serialize(next.manual)
        }
    }
}
