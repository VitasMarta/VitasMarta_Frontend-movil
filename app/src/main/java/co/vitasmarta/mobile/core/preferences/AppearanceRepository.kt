package co.vitasmarta.mobile.core.preferences

import kotlinx.coroutines.flow.Flow

enum class ThemeMode(val storedValue: String) {
    System("system"),
    Light("light"),
    Dark("dark");

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.storedValue == value } ?: System
    }
}

interface AppearanceRepository {
    val mode: Flow<ThemeMode>
    suspend fun setMode(mode: ThemeMode)
}
