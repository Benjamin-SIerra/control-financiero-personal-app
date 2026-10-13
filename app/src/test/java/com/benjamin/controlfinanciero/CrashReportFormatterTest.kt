package com.benjamin.controlfinanciero

import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportFormatterTest {
    @Test
    fun reportIncludesExceptionTypeMessageAndStackTrace() {
        val error = IllegalStateException("fallo de prueba")

        val report = CrashReportFormatter.format(error, "2026-10-12 10:00")

        assertTrue(report.contains("IllegalStateException"))
        assertTrue(report.contains("fallo de prueba"))
        assertTrue(report.contains("CrashReportFormatterTest"))
        assertTrue(report.contains("2026-10-12 10:00"))
    }

    @Test
    fun reportIsLimitedToPreventUnboundedCrashLogs() {
        val error = RuntimeException("x".repeat(50_000))

        val report = CrashReportFormatter.format(error, "ahora")

        assertTrue(report.length <= CrashReportFormatter.MAX_REPORT_CHARS)
        assertTrue(report.contains("reporte recortado"))
    }
}
