package com.benjamin.controlfinanciero.domain

import com.benjamin.controlfinanciero.data.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class FinanceLogicTest{
 @Test fun monthlyBillIsHalvedForEachSemimonthlyPay(){val bill=FixedExpense(title="Renta",amountMinor=200_000,currency="MXN",exchangeRate=1.0,frequency="MENSUAL",dueLabel="día 1");val r=calculateBudget(800_000,"QUINCENAL",listOf(bill),emptyList(),emptyList(),emptyList());assertEquals(100_000,r.reserve);assertEquals(700_000,r.free)}
 @Test fun weeklyBillStaysOneWeeklyAmountForWeeklyPay(){val bill=FixedExpense(title="Servicio",amountMinor=70_000,currency="MXN",exchangeRate=1.0,frequency="SEMANAL",dueLabel="viernes");assertEquals(70_000,calculateBudget(500_000,"SEMANAL",listOf(bill),emptyList(),emptyList(),emptyList()).reserve)}
 @Test fun quickTextCapturesAmountAndCardIntent(){val x=parseQuickExpense("Tarjeta $500 pago mensual");assertEquals(50_000,x.amountMinor);assertTrue(x.isCard);assertEquals("pago mensual",x.concept)}
 @Test fun rejectsMalformedQuickCommands(){assertThrows(IllegalArgumentException::class.java){parseQuickExpense("tacos")}}
 @Test fun parsesThousandsAndDecimalSeparators(){assertEquals(100_000,moneyToMinor("1,000.00"));assertEquals(100_000,moneyToMinor("1.000,00"));assertEquals(12_050,moneyToMinor("120,50"))}
 @Test fun salaryFundedCardPaymentIsReservedPerPaycheck(){val c=CreditCard(name="Visa",limitMinor=1_000_000,cutDay=10,dueDay=20,salaryFunded=true,expectedPaymentMinor=300_000);val r=calculateBudget(1_000_000,"QUINCENAL",emptyList(),listOf(c),emptyList(),emptyList());assertEquals(150_000,r.reserve)}
 @Test fun manualFxIsAppliedToBudget(){val bill=FixedExpense(title="Cloud",amountMinor=10_000,currency="USD",exchangeRate=20.0,frequency="MENSUAL",dueLabel="día 5");assertEquals(100_000,calculateBudget(500_000,"QUINCENAL",listOf(bill),emptyList(),emptyList(),emptyList()).reserve)}
 @Test fun goalSavingsAreSpreadAcrossRemainingPayPeriods(){val goal=SavingsGoal(name="Viaje",targetMinor=100_000,currency="MXN",deadlineEpochDay=LocalDate.now().plusDays(90).toEpochDay());assertEquals(16_667,calculateBudget(1_000_000,"QUINCENAL",emptyList(),emptyList(),listOf(goal),emptyList()).goalReserve)}
 @Test fun weeklyFixedExpenseKeepsItsPerOccurrenceAmountSeparateFromMonthlyTotal(){val bill=FixedExpense(title="Payjoy",amountMinor=100_000,currency="MXN",exchangeRate=1.0,frequency="SEMANAL",dueLabel="viernes");assertEquals(100_000,fixedExpenseAmountPerOccurrence(bill));assertEquals(433_333,fixedExpenseMonthlyEquivalent(bill))}
 @Test fun monthlyBudgetUsesWeeklyExpenseMonthlyEquivalent(){val bill=FixedExpense(title="Payjoy",amountMinor=100_000,currency="MXN",exchangeRate=1.0,frequency="SEMANAL",dueLabel="viernes");assertEquals(433_333,calculateBudget(2_000_000,"MENSUAL",listOf(bill),emptyList(),emptyList(),emptyList()).reserve)}
 @Test fun pickerMillisFormatsAsIsoCalendarDate(){val millis=LocalDate.of(2027,2,5).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();assertEquals("2027-02-05",selectedDateMillisToIsoDate(millis))}
}
