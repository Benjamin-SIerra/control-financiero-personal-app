package com.benjamin.controlfinanciero.domain

import com.benjamin.controlfinanciero.data.*
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.roundToLong

data class BudgetResult(val income:Long,val reserve:Long,val goalReserve:Long,val spent:Long,val free:Long)
data class QuickExpense(val concept:String,val amountMinor:Long,val isCard:Boolean)

fun payPeriodsPerYear(frequency:String):Double=when(frequency){"SEMANAL"->52.0;"CATORCENAL"->26.0;"QUINCENAL"->24.0;"MENSUAL"->12.0;else->error("Frecuencia de pago inválida")}
fun chargePeriodsPerYear(frequency:String):Double=when(frequency){"SEMANAL"->52.0;"CATORCENAL"->26.0;"QUINCENAL"->24.0;"MENSUAL"->12.0;else->error("Frecuencia de cobro inválida")}
fun toBaseMinor(amountMinor:Long,rate:Double):Long{require(amountMinor>=0 && rate>0 && rate.isFinite());return (amountMinor*rate).roundToLong()}
fun moneyToMinor(text:String):Long{val raw=text.trim().replace("$","").replace(" ","");val normalized=when{raw.contains(',')&&raw.contains('.')->{if(raw.lastIndexOf(',')>raw.lastIndexOf('.'))raw.replace(".","").replace(',','.')else raw.replace(",","")};raw.count{it==','}==1->{if(raw.substringAfterLast(',').length in 1..2)raw.replace(',','.')else raw.replace(",","")};else->raw};val n=normalized.toBigDecimalOrNull()?:throw IllegalArgumentException("Escribe un monto válido");require(n.signum()>=0);return n.movePointRight(2).setScale(0,java.math.RoundingMode.HALF_UP).longValueExact()}
fun parseQuickExpense(text:String):QuickExpense{
 val match=Regex("(?i)^\\s*(gasto|tarjeta)?\\s*\\$?\\s*([0-9]+(?:[.,][0-9]{1,2})?)\\s+(.+?)\\s*$").matchEntire(text) ?: throw IllegalArgumentException("Usa: Gasto $120 tacos o Tarjeta $500 comida")
 val amount=moneyToMinor(match.groupValues[2]);require(amount>0){"El gasto debe ser mayor que cero"}
 val concept=match.groupValues[3].trim();require(concept.isNotBlank())
 return QuickExpense(concept,amount,match.groupValues[1].equals("tarjeta",true))
}
fun calculateBudget(incomeMinor:Long,payFrequency:String,fixed:List<FixedExpense>,cards:List<CreditCard>,goals:List<SavingsGoal>,expenses:List<Expense>,today:LocalDate=LocalDate.now()):BudgetResult{
 require(incomeMinor>=0)
 val pays=payPeriodsPerYear(payFrequency)
 val fixedReserve=fixed.filter{it.enabled}.sumOf{(toBaseMinor(it.amountMinor,it.exchangeRate)*chargePeriodsPerYear(it.frequency)/pays).roundToLong()}
 val cardReserve=cards.filter{it.salaryFunded}.sumOf{(toBaseMinor(it.expectedPaymentMinor,it.exchangeRate)*12.0/pays).roundToLong()}
 val goalReserve=goals.sumOf { g->
   val remaining=toBaseMinor((g.targetMinor-g.savedMinor).coerceAtLeast(0),g.exchangeRate)
   val days=java.time.temporal.ChronoUnit.DAYS.between(today,LocalDate.ofEpochDay(g.deadlineEpochDay))
   val periods=ceil((days.coerceAtLeast(1)*pays)/365.2425).toLong().coerceAtLeast(1)
   ceil(remaining.toDouble()/periods).toLong()
 }
 val spent=expenses.sumOf{toBaseMinor(it.amountMinor,it.exchangeRate)}
 val reserve=fixedReserve+cardReserve
 return BudgetResult(incomeMinor,reserve,goalReserve,spent,incomeMinor-reserve-goalReserve-spent)
}

fun categoryFor(concept:String):String=when{listOf("taco","comida","super","restaurante","cafe").any{concept.contains(it,true)}->"Alimentos";listOf("gas","uber","taxi","camion","transporte").any{concept.contains(it,true)}->"Transporte";listOf("luz","agua","renta","internet","telefono").any{concept.contains(it,true)}->"Servicios";else->"Otros"}
