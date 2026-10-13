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
 private val _notice=MutableSharedFlow<String>(extraBufferCapacity=1);val notice=_notice.asSharedFlow()
 init{viewModelScope.launch{todayEpochDay.collect{day->repo.ensureCurrentIncomePeriod(LocalDate.ofEpochDay(day))}}}
 fun saveIncomePeriod(id:String,start:String,end:String,amount:String,currency:String,frequency:String)=op{
  val startDay=LocalDate.parse(start).toEpochDay();val endDay=LocalDate.parse(end).toEpochDay()
  repo.saveIncomePeriod(id.toLongOrNull()?:0,startDay,endDay,moneyToMinor(amount),currency,frequency)
  "Periodo de ingreso guardado"
 }
 fun deleteIncomePeriod(id:Long)=op{repo.deleteIncomePeriod(id);"Periodo de ingreso eliminado"}
 fun addFixed(name:String,amount:String,currency:String,rate:String,freq:String,due:String)=op{repo.addFixed(name,moneyToMinor(amount),currency,rate.toDoubleOrNull()?:1.0,freq,due);"Gasto recurrente agregado"}
 fun deleteFixed(id:Long)=op{repo.deleteFixed(id);"Gasto fijo eliminado"}
 fun addCard(name:String,limit:String,cut:String,due:String,salary:Boolean,payment:String,currency:String,rate:String)=op{repo.addCard(name,moneyToMinor(limit),cut.toInt(),due.toInt(),salary,if(payment.isBlank())0 else moneyToMinor(payment),currency,rate.toDoubleOrNull()?:1.0);"Tarjeta agregada"}
 fun spend(text:String,currency:String,rate:String,cardId:Long?,category:String)=op{val parsed=parseQuickExpense(text);val resolved=if(category=="Otros")categoryFor(parsed.concept)else category;repo.spend(parsed.concept,resolved,parsed.amountMinor,currency,rate.toDoubleOrNull()?:1.0,if(parsed.isCard)cardId else null);"Gasto guardado: ${parsed.concept}"}
 fun payCard(id:Long,amount:String)=op{repo.payCard(id,moneyToMinor(amount));"Abono registrado"}
 fun addGoal(name:String,target:String,currency:String,rate:String,deadline:String)=op{val d=LocalDate.parse(deadline).toEpochDay();repo.addGoal(name,moneyToMinor(target),currency,rate.toDoubleOrNull()?:1.0,d);"Meta creada"}
 fun contribute(id:Long,amount:String)=op{repo.contribute(id,moneyToMinor(amount));"Aporte registrado"}
 private fun op(block:suspend()->String){viewModelScope.launch{runCatching{block()}.onSuccess{_notice.emit(it)}.onFailure{_notice.emit(it.message?:"Revisa los datos")}}}
 companion object{fun factory(repo:FinanceRepository)=object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=FinanceViewModel(repo) as T}}
}
