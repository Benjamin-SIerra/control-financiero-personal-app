package com.benjamin.controlfinanciero.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.selection.SelectionContainer

private val ErrorGold = Color(0xFFC8A84C)
private val ErrorMuted = Color(0xFFB5B3AC)

@Composable
fun CrashReportScreen(
    report: String,
    clipboard: ClipboardManager,
    onContinue: () -> Unit
) {
    Surface(color = Color(0xFF0A0B0D), modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("SE DETECTÓ UN ERROR", color = ErrorGold, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(
                "La app guardó el detalle técnico en este teléfono. Copia el reporte y pégalo en el chat para que pueda identificar la causa.",
                color = ErrorMuted,
                fontSize = 14.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { clipboard.setText(AnnotatedString(report)) },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorGold, contentColor = Color.Black),
                    modifier = Modifier.weight(1f)
                ) { Text("Copiar error") }
                TextButton(onClick = onContinue, modifier = Modifier.weight(1f)) {
                    Text("Continuar", color = ErrorGold)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("REPORTE TÉCNICO", color = ErrorGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            SelectionContainer {
                Text(
                    report,
                    color = Color(0xFFE8E5DD),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Text(
                "El reporte se guarda solo en el almacenamiento privado de la app; no se envía automáticamente.",
                color = ErrorMuted,
                fontSize = 12.sp
            )
        }
    }
}
