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
 @Test fun structuredExpenseRequiresKnownCategoryAndPositiveNumericAmount(){val x=parseStructuredExpense("Alimentos","120.50");assertEquals("Alimentos",x.concept);assertEquals(12_050,x.amountMinor);assertThrows(IllegalArgumentException::class.java){parseStructuredExpense("","120")};assertThrows(IllegalArgumentException::class.java){parseStructuredExpense("Otros","0")};assertThrows(IllegalArgumentException::class.java){parseStructuredExpense("inventada","50")}}
 @Test fun carryoverAndManualCorrectionAffectOnlyComputedFreeBalance(){val result=calculateBudget(100_000,"SEMANAL",emptyList(),emptyList(),emptyList(),emptyList(),carryoverMinor=30_000,manualAdjustmentMinor=-5_000);assertEquals(125_000,result.free)}
 @Test fun targetFreeBalanceProducesPeriodScopedAdjustment(){assertEquals(-25_000,adjustmentToReachFreeBalance(100_000,30_000,105_000))}
 @Test fun signedBalanceParserAllowsNegativeBalance(){assertEquals(-12_500,moneyToMinorSigned("-125.00"));assertEquals(1_000,moneyToMinorSigned("10.00"))}
 @Test fun onlyPositiveLeftoversBecomeCarryover(){
  assertEquals(45_000,pendingCarryoverFrom(45_000))
  assertEquals(0,pendingCarryoverFrom(0))
  assertEquals(0,pendingCarryoverFrom(-8_000))
 }
 @Test fun acceptedCarryoverLiftsCurrentFreeBalanceWhileDeclinedStaysTheSame(){
  val base=calculateBudget(100_000,"SEMANAL",emptyList(),emptyList(),emptyList(),emptyList())
  val accepted=calculateBudget(100_000,"SEMANAL",emptyList(),emptyList(),emptyList(),emptyList(),carryoverMinor=pendingCarryoverFrom(45_000))
  val declined=calculateBudget(100_000,"SEMANAL",emptyList(),emptyList(),emptyList(),emptyList(),carryoverMinor=0L)
  assertEquals(100_000,base.free)
  assertEquals(145_000,accepted.free)
  assertEquals(100_000,declined.free)
 }
 @Test fun manualCorrectionMovesFreeBalanceToTheCapturedRealAmount(){
  val correction=adjustmentToReachFreeBalance(120_000,30_000,95_000)
  assertEquals(-55_000,correction)
  val applied=calculateBudget(120_000+correction+30_000,"SEMANAL",emptyList(),emptyList(),emptyList(),emptyList())
  assertEquals(95_000,applied.free)
 }
 @Test fun parsesThousandsAndDecimalSeparators(){assertEquals(100_000,moneyToMinor("1,000.00"));assertEquals(100_000,moneyToMinor("1.000,00"));assertEquals(12_050,moneyToMinor("120,50"))}
 @Test fun salaryFundedCardPaymentIsReservedPerPaycheck(){val c=CreditCard(name="Visa",limitMinor=1_000_000,cutDay=10,dueDay=20,salaryFunded=true,expectedPaymentMinor=300_000);val r=calculateBudget(1_000_000,"QUINCENAL",emptyList(),listOf(c),emptyList(),emptyList());assertEquals(150_000,r.reserve)}
 @Test fun manualFxIsAppliedToBudget(){val bill=FixedExpense(title="Cloud",amountMinor=10_000,currency="USD",exchangeRate=20.0,frequency="MENSUAL",dueLabel="día 5");assertEquals(100_000,calculateBudget(500_000,"QUINCENAL",listOf(bill),emptyList(),emptyList(),emptyList()).reserve)}
 @Test fun goalSavingsAreSpreadAcrossRemainingPayPeriods(){val goal=SavingsGoal(name="Viaje",targetMinor=100_000,currency="MXN",deadlineEpochDay=LocalDate.now().plusDays(90).toEpochDay());assertEquals(16_667,calculateBudget(1_000_000,"QUINCENAL",emptyList(),emptyList(),listOf(goal),emptyList()).goalReserve)}
 @Test fun completedOrCancelledGoalDoesNotReserveAndReleasesFreeBalance(){
  val deadline=LocalDate.now().plusDays(90).toEpochDay()
  val active=SavingsGoal(name="Viaje",targetMinor=100_000,currency="MXN",deadlineEpochDay=deadline)
  val activeBudget=calculateBudget(1_000_000,"QUINCENAL",emptyList(),emptyList(),listOf(active),emptyList())
  val completedBudget=calculateBudget(1_000_000,"QUINCENAL",emptyList(),emptyList(),listOf(active.copy(status="COMPLETED")),emptyList())
  val cancelledBudget=calculateBudget(1_000_000,"QUINCENAL",emptyList(),emptyList(),listOf(active.copy(status="CANCELLED")),emptyList())
  assertEquals(16_667,activeBudget.goalReserve)
  assertEquals(983_333,activeBudget.free)
  assertEquals(0,completedBudget.goalReserve);assertEquals(1_000_000,completedBudget.free)
  assertEquals(0,cancelledBudget.goalReserve);assertEquals(1_000_000,cancelledBudget.free)
 }
 @Test fun goalFxRateConvertsTargetIntoBaseCurrencyForReserve(){
  val goal=SavingsGoal(name="Laptop",targetMinor=100_000,currency="USD",exchangeRate=20.0,deadlineEpochDay=LocalDate.now().plusDays(90).toEpochDay())
  assertEquals(333_334,calculateBudget(1_000_000,"QUINCENAL",emptyList(),emptyList(),listOf(goal),emptyList()).goalReserve)
 }
 @Test fun manualRateConvertsForeignExpenseIntoBaseMinorUnits(){assertEquals(218_750,toBaseMinor(12_500,17.5))}
 @Test fun completedGoalIsNotReservedAfterDeadlineCalculation(){
  val goal=SavingsGoal(name="Fondo",targetMinor=50_000,currency="MXN",deadlineEpochDay=LocalDate.now().plusDays(10).toEpochDay(),status="COMPLETED")
  assertEquals(0,calculateBudget(500_000,"QUINCENAL",emptyList(),emptyList(),listOf(goal),emptyList()).goalReserve)
 }

 @Test fun monthlyBudgetUsesWeeklyExpenseMonthlyEquivalent(){val bill=FixedExpense(title="Payjoy",amountMinor=100_000,currency="MXN",exchangeRate=1.0,frequency="SEMANAL",dueLabel="viernes");assertEquals(433_333,calculateBudget(2_000_000,"MENSUAL",listOf(bill),emptyList(),emptyList(),emptyList()).reserve)}
 @Test fun pickerMillisFormatsAsIsoCalendarDate(){val millis=LocalDate.of(2027,2,5).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();assertEquals("2027-02-05",selectedDateMillisToIsoDate(millis))}
}
