package co.vitasmarta.mobile.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.vitasmarta.mobile.core.designsystem.VitasMartaTheme
import co.vitasmarta.mobile.core.preferences.AppLanguage
import co.vitasmarta.mobile.feature.settings.presentation.AppearanceViewModel
import co.vitasmarta.mobile.navigation.VitasMartaApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val appearance: AppearanceViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { !appearance.state.value.ready }
        setContent {
            val state by appearance.state.collectAsStateWithLifecycle()
            val dark = state.mode.isDark(isSystemInDarkTheme())
            SideEffect {
                val transparent = android.graphics.Color.TRANSPARENT
                val barStyle = if (dark) {
                    SystemBarStyle.dark(transparent)
                } else {
                    SystemBarStyle.light(transparent, transparent)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
            }
            if (state.ready) {
                VitasMartaTheme(darkTheme = dark) {
                    VitasMartaApp(state, appearance::select)
                }
            }
        }
    }
}
