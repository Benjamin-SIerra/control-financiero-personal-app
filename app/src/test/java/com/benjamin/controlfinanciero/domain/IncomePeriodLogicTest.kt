package com.benjamin.controlfinanciero.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class IncomePeriodLogicTest {
    @Test fun periodBecomesHistoricalOnlyAfterItsEndDate() {
        val start = LocalDate.of(2027, 4, 1).toEpochDay()
        val end = LocalDate.of(2027, 4, 14).toEpochDay()
        assertEquals(IncomePeriodStatus.UPCOMING, incomePeriodStatus(start, end, start - 1))
        assertEquals(IncomePeriodStatus.ACTIVE, incomePeriodStatus(start, end, end))
        assertEquals(IncomePeriodStatus.HISTORICAL, incomePeriodStatus(start, end, end + 1))
    }

    @Test fun dateRangesAreInclusiveAndAdjacentPeriodsDoNotOverlap() {
        val aStart = LocalDate.of(2027, 1, 1).toEpochDay()
        val aEnd = LocalDate.of(2027, 1, 14).toEpochDay()
        val adjacentStart = aEnd + 1
        assertFalse(incomePeriodsOverlap(aStart, aEnd, adjacentStart, adjacentStart + 13))
        assertTrue(incomePeriodsOverlap(aStart, aEnd, aEnd, aEnd + 5))
    }

    @Test fun defaultEndDateUsesFrequencyAndSupportsCalendarMonth() {
        val start = LocalDate.of(2027, 1, 31).toEpochDay()
        assertEquals(start + 6, defaultIncomePeriodEnd(start, "SEMANAL"))
        assertEquals(start + 13, defaultIncomePeriodEnd(start, "CATORCENAL"))
        assertEquals(start + 14, defaultIncomePeriodEnd(start, "QUINCENAL"))
        assertEquals(LocalDate.of(2027, 2, 27).toEpochDay(), defaultIncomePeriodEnd(start, "MENSUAL"))
    }

    @Test fun overduePeriodStartsNextIndependentPeriodTodayWithoutCopyingItsIncome() {
        val today = LocalDate.of(2027, 4, 20).toEpochDay()
        val priorEnd = LocalDate.of(2027, 4, 14).toEpochDay()
        assertEquals(today, nextIncomePeriodStart(today, priorEnd))
        assertEquals(priorEnd + 1, nextIncomePeriodStart(priorEnd + 1, priorEnd))
    }
}
