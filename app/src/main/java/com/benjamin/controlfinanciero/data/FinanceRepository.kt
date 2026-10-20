package com.benjamin.controlfinanciero.data

import androidx.room.withTransaction
import com.benjamin.controlfinanciero.domain.*
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FinanceRepository(private val db:FinanceDatabase){
 private val dao=db.dao()
 val profile:Flow<Profile?> = dao.profile()
 val incomePeriods:Flow<List<IncomePeriod>> = dao.incomePeriods()
 val fixed:Flow<List<FixedExpense>> = dao.fixed()
 val cards:Flow<List<CreditCard>> = dao.cards()
 val goals:Flow<List<SavingsGoal>> = dao.goals()
 fun periodBalance(periodId:Long):Flow<PeriodBalanceAdjustment?> = dao.periodBalance(periodId)

 fun expenses(period:IncomePeriod?,p:Profile?):Flow<List<Expense>>{
  val zone=ZoneId.systemDefault()
  val range=if(period!=null){
   period.startEpochDay to period.endEpochDay
  }else{
   val millis=periodRange(p)
   val start=LocalDate.ofInstant(java.time.Instant.ofEpochMilli(millis.first),zone).toEpochDay()
   val end=LocalDate.ofInstant(java.time.Instant.ofEpochMilli(millis.second-1),zone).toEpochDay()
   start to end
  }
  val from=LocalDate.ofEpochDay(range.first).atStartOfDay(zone).toInstant().toEpochMilli()
  val to=LocalDate.ofEpochDay(range.second).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
  return dao.expenses(from,to)
 }

 suspend fun saveIncomePeriod(id:Long,startEpochDay:Long,endEpochDay:Long,amount:Long,currency:String,frequency:String){
  require(startEpochDay<=endEpochDay){"La fecha de inicio debe ser anterior o igual a la fecha de fin"}
  require(amount>=0){"El ingreso no puede ser negativo"}
  require(currency.length==3){"Usa un código de moneda de 3 letras"}
  require(frequency in setOf("SEMANAL","CATORCENAL","QUINCENAL","MENSUAL")){"Frecuencia de pago inválida"}
  db.withTransaction{
   val all=dao.incomePeriodsOnce()
   require(all.none{it.id!=id&&incomePeriodsOverlap(startEpochDay,endEpochDay,it.startEpochDay,it.endEpochDay)}){"El rango se traslapa con otro periodo guardado"}
   val normalizedCurrency=currency.uppercase()
   val old=dao.profileOnce()
   require(old==null||normalizedCurrency==old.currency){"Los periodos deben usar la moneda base configurada (${old?.currency})"}
   val savedId=dao.saveIncomePeriod(IncomePeriod(id=id,startEpochDay=startEpochDay,endEpochDay=endEpochDay,incomeMinor=amount,currency=normalizedCurrency,frequency=frequency))
   val persistedId=if(id==0L)savedId else id
   if(dao.periodBalanceOnce(persistedId)==null){
    val newPeriod=IncomePeriod(id=persistedId,startEpochDay=startEpochDay,endEpochDay=endEpochDay,incomeMinor=amount,currency=normalizedCurrency,frequency=frequency)
    dao.savePeriodBalance(makeRolloverState(newPeriod,all.filter{it.id!=id}+newPeriod,LocalDate.now()))
   }
   val today=LocalDate.now().toEpochDay()
   if(old==null||today in startEpochDay..endEpochDay)dao.saveProfile(Profile(incomeMinor=amount,currency=normalizedCurrency,payFrequency=frequency,anchorEpochDay=startEpochDay).copy(id=old?.id?:1))
  }
 }

 suspend fun ensureCurrentIncomePeriod(today:LocalDate=LocalDate.now()){
  val todayEpoch=today.toEpochDay()
  db.withTransaction{
   val periods=dao.incomePeriodsOnce()
   val active=periods.filter{it.startEpochDay<=todayEpoch&&it.endEpochDay>=todayEpoch}.maxByOrNull{it.startEpochDay}
   if(active!=null){
    val state=dao.periodBalanceOnce(active.id)
    if(state==null||!state.rolloverResolved)dao.savePeriodBalance(makeRolloverState(active,periods,today))
    return@withTransaction
   }
   if(periods.any{it.startEpochDay>todayEpoch})return@withTransaction
   val previous=periods.filter{it.endEpochDay<todayEpoch}.maxByOrNull{it.endEpochDay}
   val profile=dao.profileOnce()
   val frequency=previous?.frequency?:profile?.payFrequency?:"QUINCENAL"
   val currency=previous?.currency?:profile?.currency?:"MXN"
   val start=nextIncomePeriodStart(todayEpoch,previous?.endEpochDay)
   val newPeriod=IncomePeriod(startEpochDay=start,endEpochDay=defaultIncomePeriodEnd(start,frequency),incomeMinor=0,currency=currency,frequency=frequency)
   val newId=dao.saveIncomePeriod(newPeriod)
   dao.savePeriodBalance(makeRolloverState(newPeriod.copy(id=newId),periods+newPeriod.copy(id=newId),today))
  }
 }

 private suspend fun makeRolloverState(period:IncomePeriod,periods:List<IncomePeriod>,today:LocalDate):PeriodBalanceAdjustment{
  val previous=periods.filter{it.id!=period.id&&it.endEpochDay<period.startEpochDay}.maxByOrNull{it.endEpochDay}
  val active=today.toEpochDay() in period.startEpochDay..period.endEpochDay
  if(!active||previous==null)return PeriodBalanceAdjustment(period.id,rolloverResolved=true)
  val previousBalance=freeBalanceSnapshot(previous)
  val pending=previousBalance.coerceAtLeast(0L)
  return PeriodBalanceAdjustment(periodId=period.id,pendingCarryoverMinor=pending,rolloverResolved=pending==0L)
 }

 private suspend fun freeBalanceSnapshot(period:IncomePeriod):Long{
  val zone=ZoneId.systemDefault()
  val from=LocalDate.ofEpochDay(period.startEpochDay).atStartOfDay(zone).toInstant().toEpochMilli()
  val to=LocalDate.ofEpochDay(period.endEpochDay).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
  val expenses=dao.expensesOnce(from,to)
  val state=dao.periodBalanceOnce(period.id)?:PeriodBalanceAdjustment(period.id,rolloverResolved=true)
  return calculateBudget(period.incomeMinor,period.frequency,fixed.first(),cards.first(),goals.first(),expenses,LocalDate.ofEpochDay(period.endEpochDay),state.carryoverMinor,state.manualAdjustmentMinor).free
 }

 suspend fun decideCarryover(periodId:Long,accept:Boolean){db.withTransaction{
  val today=LocalDate.now().toEpochDay();val period=dao.incomePeriodsOnce().firstOrNull{it.id==periodId&&today in it.startEpochDay..it.endEpochDay}?:error("El arrastre solo se puede decidir en el periodo activo")
  val state=dao.periodBalanceOnce(period.id)?:error("No hay remanente pendiente")
  require(!state.rolloverResolved&&state.pendingCarryoverMinor>0){"No hay remanente pendiente de decidir"}
  dao.savePeriodBalance(state.copy(carryoverMinor=if(accept)state.pendingCarryoverMinor else 0L,pendingCarryoverMinor=0L,rolloverResolved=true))
 }}

 suspend fun setFreeBalanceForCurrentPeriod(periodId:Long,targetFreeMinor:Long){db.withTransaction{
  val today=LocalDate.now().toEpochDay();val period=dao.incomePeriodsOnce().firstOrNull{it.id==periodId&&today in it.startEpochDay..it.endEpochDay}?:error("El ajuste solo aplica al periodo activo")
  val state=dao.periodBalanceOnce(period.id)?:PeriodBalanceAdjustment(period.id,rolloverResolved=true)
  require(state.pendingCarryoverMinor==0L||state.rolloverResolved){"Resuelve primero el remanente pendiente"}
  val zone=ZoneId.systemDefault();val from=LocalDate.ofEpochDay(period.startEpochDay).atStartOfDay(zone).toInstant().toEpochMilli();val to=LocalDate.ofEpochDay(period.endEpochDay).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
  val expenses=dao.expensesOnce(from,to)
  val baseFree=calculateBudget(period.incomeMinor,period.frequency,fixed.first(),cards.first(),goals.first(),expenses,LocalDate.now(),carryoverMinor=0,manualAdjustmentMinor=0).free
  val adjustment=adjustmentToReachFreeBalance(baseFree,state.carryoverMinor,targetFreeMinor)
  dao.savePeriodBalance(state.copy(manualAdjustmentMinor=adjustment))
 }}

 suspend fun deleteIncomePeriod(id:Long)=db.withTransaction{dao.deleteIncomePeriod(id)}

 suspend fun addFixed(name:String,amount:Long,currency:String,rate:Double,frequency:String,due:String){require(name.isNotBlank()&&amount>0&&rate>0&&rate.isFinite());require(currency.length==3);require(frequency in setOf("SEMANAL","CATORCENAL","QUINCENAL","MENSUAL"));dao.addFixed(FixedExpense(title=name.trim(),amountMinor=amount,currency=currency.uppercase(),exchangeRate=rate,frequency=frequency,dueLabel=due.trim()))}
 suspend fun deleteFixed(id:Long)=dao.deleteFixed(id)
 suspend fun addCard(name:String,limit:Long,cut:Int,due:Int,salary:Boolean,payment:Long,currency:String,rate:Double){require(name.isNotBlank()&&limit>0&&cut in 1..31&&due in 1..31&&payment>=0&&rate>0&&rate.isFinite()&&currency.length==3);dao.addCard(CreditCard(name=name.trim(),limitMinor=limit,cutDay=cut,dueDay=due,salaryFunded=salary,expectedPaymentMinor=payment,currency=currency.uppercase(),exchangeRate=rate))}
 suspend fun spend(concept:String,category:String,amount:Long,currency:String,rate:Double,cardId:Long?){require(concept.isNotBlank()&&category.isNotBlank()&&amount>0&&rate>0&&rate.isFinite()&&currency.length==3);db.withTransaction{if(cardId!=null){val c=dao.card(cardId)?:error("Tarjeta no encontrada");val base=toBaseMinor(amount,rate);val cardAmount=(base/c.exchangeRate).toLong();check(dao.changeCardUsed(cardId,cardAmount)==1){"El gasto supera el crédito disponible"}};dao.addExpense(Expense(concept=concept.trim(),category=category,amountMinor=amount,currency=currency.uppercase(),exchangeRate=rate,cardId=cardId))}}
 suspend fun payCard(id:Long,amount:Long){require(amount>0);db.withTransaction{val c=dao.card(id)?:error("Tarjeta no encontrada");require(amount<=c.usedMinor){"El abono supera el saldo utilizado"};check(dao.changeCardUsed(id,-amount)==1);dao.addPayment(CardPayment(cardId=id,amountMinor=amount))}}
 suspend fun addGoal(name:String,target:Long,currency:String,rate:Double,deadline:Long){
  require(name.isNotBlank()&&target>0&&rate>0&&rate.isFinite()&&deadline>=LocalDate.now().toEpochDay()&&currency.length==3)
  dao.addGoal(SavingsGoal(name=name.trim(),targetMinor=target,currency=currency.uppercase(),exchangeRate=rate,deadlineEpochDay=deadline))
 }
 suspend fun updateGoal(id:Long,name:String,target:Long,currency:String,rate:Double,deadline:Long){
  require(name.isNotBlank()&&target>0&&rate>0&&rate.isFinite()&&deadline>=LocalDate.now().toEpochDay()&&currency.length==3)
  db.withTransaction{
   val goal=dao.goals().first().firstOrNull{it.id==id}?:error("Meta no encontrada")
   require(goal.status=="ACTIVE"){"Solo se pueden editar metas activas"}
   require(target>=goal.savedMinor){"El objetivo no puede ser menor a lo que ya ahorraste"}
   require(goal.savedMinor==0L||currency.equals(goal.currency,ignoreCase=true)){"No cambies la divisa después de registrar aportaciones; crea otra meta para evitar reinterpretar el ahorro existente"}
   check(dao.updateGoal(id,name.trim(),target,goal.savedMinor,currency.uppercase(),rate,deadline)==1){"No se pudo actualizar la meta"}
  }
 }
 suspend fun contribute(id:Long,amount:Long){require(amount>0);db.withTransaction{check(dao.contributeGoal(id,amount)==1){"El aporte supera el monto restante o la meta ya no está activa"}}}
 suspend fun setGoalStatus(id:Long,status:String){require(status in setOf("CANCELLED","COMPLETED"));check(dao.setGoalStatus(id,status)==1){"La meta no está activa"}}
 suspend fun deleteGoal(id:Long)=db.withTransaction{dao.deleteGoal(id)}
}

fun periodRange(p:Profile?):Pair<Long,Long>{val now=System.currentTimeMillis();if(p==null)return (now-30L*86400000) to (now+1);val duration=(365.2425/payPeriodsPerYear(p.payFrequency)*86400000.0).toLong().coerceAtLeast(1);val anchor=LocalDate.ofEpochDay(p.anchorEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();val index=Math.floorDiv(now-anchor,duration);val start=anchor+index*duration;return start to start+duration}
