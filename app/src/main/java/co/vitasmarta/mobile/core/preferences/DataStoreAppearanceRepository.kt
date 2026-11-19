package co.vitasmarta.mobile.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

private val Context.appearanceStore by preferencesDataStore(name = "appearance")

@Singleton
class DataStoreAppearanceRepository @Inject constructor(@ApplicationContext context: Context) :
    AppearanceRepository {
    private val store = context.appearanceStore
    private val key = stringPreferencesKey("theme_mode")
    override val mode = store.data.map { ThemeMode.fromStored(it[key]) }

    override suspend fun setMode(mode: ThemeMode) {
        store.edit { values ->
            if (mode == ThemeMode.System) values.remove(key) else values[key] = mode.storedValue
        }
    }
}
