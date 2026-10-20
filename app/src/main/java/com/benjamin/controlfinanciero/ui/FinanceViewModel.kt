package com.benjamin.controlfinanciero.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.benjamin.controlfinanciero.data.*
import com.benjamin.controlfinanciero.domain.*
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(private val repo:FinanceRepository):ViewModel(){
 val profile=repo.profile.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
 val incomePeriods=repo.incomePeriods.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val fixed=repo.fixed.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val cards=repo.cards.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val goals=repo.goals.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val todayEpochDay=flow{while(true){emit(LocalDate.now().toEpochDay());delay(30_000)}}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),LocalDate.now().toEpochDay())
 val activeIncomePeriod=combine(repo.incomePeriods,todayEpochDay){periods,today->periods.filter{it.startEpochDay<=today&&it.endEpochDay>=today}.maxByOrNull{it.startEpochDay}}
  .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
 val expenses=combine(activeIncomePeriod,profile){period,p->period to p}.flatMapLatest{(period,p)->repo.expenses(period,p)}
  .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val periodBalance=activeIncomePeriod.flatMapLatest{period->period?.let{repo.periodBalance(it.id)}?:flowOf(null)}
  .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
 private val _notice=MutableSharedFlow<String>(extraBufferCapacity=1);val notice=_notice.asSharedFlow()
 init{viewModelScope.launch{todayEpochDay.collect{day->repo.ensureCurrentIncomePeriod(LocalDate.ofEpochDay(day))}}}
 fun saveIncomePeriod(id:String,start:String,end:String,amount:String,currency:String,frequency:String)=op{
  val startDay=LocalDate.parse(start).toEpochDay();val endDay=LocalDate.parse(end).toEpochDay()
  repo.saveIncomePeriod(id.toLongOrNull()?:0,startDay,endDay,moneyToMinor(amount),currency,frequency)
  "Periodo de ingreso guardado"
 }
 fun deleteIncomePeriod(id:Long)=op{repo.deleteIncomePeriod(id);"Periodo de ingreso eliminado"}
 fun addFixed(name:String,amount:String,currency:String,rate:String,freq:String,due:String)=op{repo.addFixed(name,moneyToMinor(amount),currency,rate.toDoubleOrNull()?:throw IllegalArgumentException("Escribe el tipo de cambio"),freq,due);"Gasto recurrente agregado"}
 fun deleteFixed(id:Long)=op{repo.deleteFixed(id);"Gasto fijo eliminado"}
 fun addCard(name:String,limit:String,cut:String,due:String,salary:Boolean,payment:String,currency:String,rate:String)=op{repo.addCard(name,moneyToMinor(limit),cut.toInt(),due.toInt(),salary,if(payment.isBlank())0 else moneyToMinor(payment),currency,rate.toDoubleOrNull()?:throw IllegalArgumentException("Escribe el tipo de cambio"));"Tarjeta agregada"}
 fun spend(category:String,amount:String,currency:String,rate:String,cardId:Long?,isCard:Boolean)=op{
  val parsed=parseStructuredExpense(category,amount)
  val selectedCard=if(isCard)cardId?:throw IllegalArgumentException("Primero agrega una tarjeta para cargar este gasto") else null
  repo.spend(parsed.concept,category,parsed.amountMinor,currency,rate.toDoubleOrNull()?:throw IllegalArgumentException("Escribe el tipo de cambio"),selectedCard)
  "Gasto guardado: $category"
 }
 fun decideCarryover(accept:Boolean)=op{val period=activeIncomePeriod.value?:error("No hay un periodo activo");repo.decideCarryover(period.id,accept);if(accept)"Remanente añadido al saldo del periodo" else "Remanente anterior descartado"}
 fun setCurrentFreeBalance(amount:String)=op{val period=activeIncomePeriod.value?:error("No hay un periodo activo");repo.setFreeBalanceForCurrentPeriod(period.id,moneyToMinorSigned(amount));"Saldo libre del periodo actual actualizado"}

 fun payCard(id:Long,amount:String)=op{repo.payCard(id,moneyToMinor(amount));"Abono registrado"}
 fun addGoal(name:String,target:String,currency:String,rate:String,deadline:String)=op{val d=LocalDate.parse(deadline).toEpochDay();repo.addGoal(name,moneyToMinor(target),currency,rate.toDoubleOrNull()?:throw IllegalArgumentException("Escribe el tipo de cambio"),d);"Meta creada"}
 fun updateGoal(id:Long,name:String,target:String,currency:String,rate:String,deadline:String)=op{val d=LocalDate.parse(deadline).toEpochDay();repo.updateGoal(id,name,moneyToMinor(target),currency,rate.toDoubleOrNull()?:throw IllegalArgumentException("Escribe el tipo de cambio"),d);"Meta actualizada"}
 fun setGoalStatus(id:Long,status:String)=op{repo.setGoalStatus(id,status);if(status=="COMPLETED")"Meta completada; apartado liberado" else "Meta cancelada; apartado liberado"}
 fun deleteGoal(id:Long)=op{repo.deleteGoal(id);"Meta eliminada; apartado liberado"}
 fun contribute(id:Long,amount:String)=op{repo.contribute(id,moneyToMinor(amount));"Aporte registrado"}
 private fun op(block:suspend()->String){viewModelScope.launch{runCatching{block()}.onSuccess{_notice.emit(it)}.onFailure{_notice.emit(it.message?:"Revisa los datos")}}}
 companion object{fun factory(repo:FinanceRepository)=object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=FinanceViewModel(repo) as T}}
}
