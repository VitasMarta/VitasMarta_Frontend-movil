package co.vitasmarta.mobile.core.preferences

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** Non-sensitive app language; credentials never enter these preferences. */
object AppLanguage {
    private const val FILE = "app_language"
    private const val KEY = "language"

    fun wrap(context: Context): Context {
        if (Build.VERSION.SDK_INT >= 33) return context
        val stored = context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
            ?: return context
        val locale = Locale.forLanguageTag(stored)
        Locale.setDefault(locale)
        return context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(locale) }
        )
    }

    fun current(context: Context): String {
        val selected = if (Build.VERSION.SDK_INT >= 33) {
            val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
            if (locales.isEmpty) null else locales[0].toLanguageTag()
        } else {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(KEY, null)
        }
        val tag = selected ?: context.resources.configuration.locales[0].toLanguageTag()
        return if (tag.startsWith("en", ignoreCase = true)) "en-US" else "es-CO"
    }

    fun apply(context: Context, language: String) {
        require(language in setOf("es-CO", "en-US"))
        Locale.setDefault(Locale.forLanguageTag(language))
        if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(language)
        } else {
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit {
                putString(KEY, language)
            }
        }
    }
}
