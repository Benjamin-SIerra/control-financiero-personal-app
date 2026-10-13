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
 val fixed:Flow<List<FixedExpense>> = dao.fixed()
 val cards:Flow<List<CreditCard>> = dao.cards()
 val goals:Flow<List<SavingsGoal>> = dao.goals()
 fun expenses(p:Profile?):Flow<List<Expense>>{val range=periodRange(p);return dao.expenses(range.first,range.second)}
 suspend fun saveIncome(amount:Long,currency:String,frequency:String){require(amount>0);require(currency.length==3);require(frequency in setOf("SEMANAL","CATORCENAL","QUINCENAL","MENSUAL"));val old=profile.first();dao.saveProfile(Profile(incomeMinor=amount,currency=currency.uppercase(),payFrequency=frequency,anchorEpochDay=old?.anchorEpochDay?.takeIf{it>0}?:LocalDate.now().toEpochDay()))}
 suspend fun addFixed(name:String,amount:Long,currency:String,rate:Double,frequency:String,due:String){require(name.isNotBlank()&&amount>0&&rate>0);require(currency.length==3);require(frequency in setOf("SEMANAL","CATORCENAL","QUINCENAL","MENSUAL"));dao.addFixed(FixedExpense(title=name.trim(),amountMinor=amount,currency=currency.uppercase(),exchangeRate=rate,frequency=frequency,dueLabel=due.trim()))}
 suspend fun deleteFixed(id:Long)=dao.deleteFixed(id)
 suspend fun addCard(name:String,limit:Long,cut:Int,due:Int,salary:Boolean,payment:Long,currency:String,rate:Double){require(name.isNotBlank()&&limit>0&&cut in 1..31&&due in 1..31&&payment>=0&&rate>0&&currency.length==3);dao.addCard(CreditCard(name=name.trim(),limitMinor=limit,cutDay=cut,dueDay=due,salaryFunded=salary,expectedPaymentMinor=payment,currency=currency.uppercase(),exchangeRate=rate))}
 suspend fun spend(concept:String,category:String,amount:Long,currency:String,rate:Double,cardId:Long?){require(concept.isNotBlank()&&category.isNotBlank()&&amount>0&&rate>0&&currency.length==3);db.withTransaction{if(cardId!=null){val c=dao.card(cardId)?:error("Tarjeta no encontrada");val base=toBaseMinor(amount,rate);val cardAmount=(base/c.exchangeRate).toLong();check(dao.changeCardUsed(cardId,cardAmount)==1){"El gasto supera el crédito disponible"}};dao.addExpense(Expense(concept=concept.trim(),category=category,amountMinor=amount,currency=currency.uppercase(),exchangeRate=rate,cardId=cardId))}}
 suspend fun payCard(id:Long,amount:Long){require(amount>0);db.withTransaction{val c=dao.card(id)?:error("Tarjeta no encontrada");require(amount<=c.usedMinor){"El abono supera el saldo utilizado"};check(dao.changeCardUsed(id,-amount)==1);dao.addPayment(CardPayment(cardId=id,amountMinor=amount))}}
 suspend fun addGoal(name:String,target:Long,currency:String,rate:Double,deadline:Long){require(name.isNotBlank()&&target>0&&rate>0&&deadline>=LocalDate.now().toEpochDay()&&currency.length==3);dao.addGoal(SavingsGoal(name=name.trim(),targetMinor=target,currency=currency.uppercase(),exchangeRate=rate,deadlineEpochDay=deadline))}
 suspend fun contribute(id:Long,amount:Long){require(amount>0);check(dao.contributeGoal(id,amount)==1){"El aporte supera el monto restante de la meta"}}
}
fun periodRange(p:Profile?):Pair<Long,Long>{val now=System.currentTimeMillis();if(p==null)return (now-30L*86400000) to (now+1);val duration=(365.2425/payPeriodsPerYear(p.payFrequency)*86400000.0).toLong().coerceAtLeast(1);val anchor=LocalDate.ofEpochDay(p.anchorEpochDay).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();val index=Math.floorDiv(now-anchor,duration);val start=anchor+index*duration;return start to start+duration}
