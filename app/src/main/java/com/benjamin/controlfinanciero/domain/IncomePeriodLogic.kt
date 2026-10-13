package com.benjamin.controlfinanciero.domain

import java.time.LocalDate

enum class IncomePeriodStatus { UPCOMING, ACTIVE, HISTORICAL }

fun incomePeriodStatus(startEpochDay: Long, endEpochDay: Long, todayEpochDay: Long): IncomePeriodStatus {
    require(startEpochDay <= endEpochDay) { "La fecha de inicio debe ser anterior o igual a la fecha de fin" }
    return when {
        todayEpochDay < startEpochDay -> IncomePeriodStatus.UPCOMING
        todayEpochDay > endEpochDay -> IncomePeriodStatus.HISTORICAL
        else -> IncomePeriodStatus.ACTIVE
    }
}

fun incomePeriodsOverlap(startA: Long, endA: Long, startB: Long, endB: Long): Boolean {
    require(startA <= endA && startB <= endB)
    return startA <= endB && startB <= endA
}

fun defaultIncomePeriodEnd(startEpochDay: Long, frequency: String): Long {
    val start = LocalDate.ofEpochDay(startEpochDay)
    return when (frequency) {
        "SEMANAL" -> start.plusDays(6)
        "CATORCENAL" -> start.plusDays(13)
        "QUINCENAL" -> start.plusDays(14)
        "MENSUAL" -> start.plusMonths(1).minusDays(1)
        else -> error("Frecuencia de pago inválida")
    }.toEpochDay()
}

/** If the previous period has already expired, start a fresh period today rather than inventing income for missed days. */
fun nextIncomePeriodStart(todayEpochDay: Long, previousEndEpochDay: Long?): Long =
    maxOf(todayEpochDay, (previousEndEpochDay ?: (todayEpochDay - 1)) + 1)
