package com.benjamin.controlfinanciero.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import com.benjamin.controlfinanciero.CrashReporter
import com.benjamin.controlfinanciero.data.FinanceDatabase
import com.benjamin.controlfinanciero.data.FinanceRepository

class MainActivity : ComponentActivity() {
    private val vm by viewModels<FinanceViewModel> {
        FinanceViewModel.factory(FinanceRepository(FinanceDatabase.create(applicationContext)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashReporter.install(applicationContext)
        setContent {
            var crashReport by remember { mutableStateOf(CrashReporter.read(applicationContext)) }
            FinanceTheme {
                val report = crashReport
                if (report == null) {
                    FinanceApp(vm)
                } else {
                    CrashReportScreen(
                        report = report,
                        clipboard = LocalClipboardManager.current,
                        onContinue = {
                            CrashReporter.clear(applicationContext)
                            crashReport = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FinanceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFC8A84C),
            onPrimary = Color(0xFF111111),
            background = Color(0xFF0A0B0D),
            surface = Color(0xFF15171A),
            onSurface = Color(0xFFF3F0E8),
            secondary = Color(0xFF55A57A),
            error = Color(0xFFD26F6F)
        ),
        content = content
    )
}
