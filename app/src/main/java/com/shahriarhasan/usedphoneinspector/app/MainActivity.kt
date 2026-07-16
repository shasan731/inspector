package com.shahriarhasan.usedphoneinspector.app

import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.shahriarhasan.usedphoneinspector.core.design.InspectorTheme
import com.shahriarhasan.usedphoneinspector.core.model.AppLanguage
import com.shahriarhasan.usedphoneinspector.navigation.InspectorApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by viewModel.settings.collectAsState()
            LaunchedEffect(settings.language) { applyLanguage(settings.language) }
            InspectorTheme(settings.themeMode, settings.dynamicColor) {
                InspectorApp(onboardingComplete = settings.onboardingComplete)
            }
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val tags = when (language) {
            AppLanguage.SYSTEM -> ""
            AppLanguage.ENGLISH -> "en"
            AppLanguage.BANGLA -> "bn"
        }
        getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(tags)
    }
}

