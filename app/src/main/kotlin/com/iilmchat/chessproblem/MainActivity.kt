package com.iilmchat.chessproblem

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.iilmchat.chessproblem.core.model.AppLanguage
import com.iilmchat.chessproblem.core.model.ThemeMode
import com.iilmchat.chessproblem.data.local.LocalePreferences
import com.iilmchat.chessproblem.ui.theme.ChessProblemTheme
import com.iilmchat.chessproblem.ui.viewmodel.CatalogViewModel
import com.iilmchat.chessproblem.ui.viewmodel.SettingsViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = LocalePreferences(newBase).language()
        if (language == AppLanguage.SYSTEM) {
            super.attachBaseContext(newBase)
            return
        }

        val locale = Locale.forLanguageTag(if (language == AppLanguage.RU) "ru" else "en")
        Locale.setDefault(locale)
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as ChessProblemApp).container

        // KI-015: тема применяется до первой композиции — SharedPreferences
        // читаются синхронно, в отличие от потока DataStore.
        applySystemBars(resolveDarkTheme(container.settingsRepository.startupTheme()))

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = AppViewModelFactory {
                    SettingsViewModel(
                        settingsRepository = container.settingsRepository,
                        progressRepository = container.progressRepository,
                        hintRepository = container.hintRepository,
                    )
                },
            )
            val catalogViewModel: CatalogViewModel = viewModel(
                factory = AppViewModelFactory {
                    CatalogViewModel(
                        puzzleRepository = container.puzzleRepository,
                        progressRepository = container.progressRepository,
                    )
                },
            )
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val darkTheme = when (settings.settings.theme) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            SideEffect { applySystemBars(darkTheme) }

            ChessProblemTheme(darkTheme = darkTheme) {
                AppNavHost(
                    container = container,
                    catalogViewModel = catalogViewModel,
                    settingsViewModel = settingsViewModel,
                    onLanguageChanged = { language ->
                        container.localePreferences.setLanguage(language)
                        settingsViewModel.setLanguage(language)
                        Handler(Looper.getMainLooper()).postDelayed(
                            { recreate() },
                            160L,
                        )
                    },
                )
            }
        }
    }

    private fun resolveDarkTheme(theme: ThemeMode): Boolean = when (theme) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM ->
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
                Configuration.UI_MODE_NIGHT_YES
    }

    private fun applySystemBars(darkTheme: Boolean) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
}
