package com.benjamin.controlfinanciero

/** Builds a bounded, local-only diagnostic report for an uncaught app failure. */
object CrashReportFormatter {
    const val MAX_REPORT_CHARS = 24_000
    private const val TRUNCATION_NOTICE = "\n\n[reporte recortado para mantenerlo manejable]"

    fun format(error: Throwable, timestamp: String): String {
        val full = buildString {
            appendLine("ERROR DE LA APP")
            appendLine("Fecha: $timestamp")
            appendLine("Tipo: ${error.javaClass.name}")
            appendLine("Mensaje: ${error.message ?: "(sin mensaje)"}")
            appendLine()
            appendLine("DETALLE TÉCNICO")
            append(error.stackTraceToString())
        }
        if (full.length <= MAX_REPORT_CHARS) return full
        val allowed = MAX_REPORT_CHARS - TRUNCATION_NOTICE.length
        return full.take(allowed) + TRUNCATION_NOTICE
    }
}
