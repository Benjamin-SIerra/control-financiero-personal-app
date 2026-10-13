package com.benjamin.controlfinanciero.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.benjamin.controlfinanciero.data.*
import com.benjamin.controlfinanciero.domain.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FinanceViewModel(private val repo:FinanceRepository):ViewModel(){
 val profile=repo.profile.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),null)
 val fixed=repo.fixed.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val cards=repo.cards.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val goals=repo.goals.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 val expenses=profile.flatMapLatest{repo.expenses(it)}.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
 private val _notice=MutableSharedFlow<String>(extraBufferCapacity=1);val notice=_notice.asSharedFlow()
 fun saveIncome(amount:String,currency:String,freq:String)=op{repo.saveIncome(moneyToMinor(amount),currency,freq);"Ingreso guardado"}
 fun addFixed(name:String,amount:String,currency:String,rate:String,freq:String,due:String)=op{repo.addFixed(name,moneyToMinor(amount),currency,rate.toDoubleOrNull()?:1.0,freq,due);"Gasto recurrente agregado"}
 fun deleteFixed(id:Long)=op{repo.deleteFixed(id);"Gasto fijo eliminado"}
 fun addCard(name:String,limit:String,cut:String,due:String,salary:Boolean,payment:String,currency:String,rate:String)=op{repo.addCard(name,moneyToMinor(limit),cut.toInt(),due.toInt(),salary,if(payment.isBlank())0 else moneyToMinor(payment),currency,rate.toDoubleOrNull()?:1.0);"Tarjeta agregada"}
 fun spend(text:String,currency:String,rate:String,cardId:Long?,category:String)=op{val parsed=parseQuickExpense(text);val resolved=if(category=="Otros")categoryFor(parsed.concept)else category;repo.spend(parsed.concept,resolved,parsed.amountMinor,currency,rate.toDoubleOrNull()?:1.0,if(parsed.isCard)cardId else null);"Gasto guardado: ${parsed.concept}"}
 fun payCard(id:Long,amount:String)=op{repo.payCard(id,moneyToMinor(amount));"Abono registrado"}
 fun addGoal(name:String,target:String,currency:String,rate:String,deadline:String)=op{val d=java.time.LocalDate.parse(deadline).toEpochDay();repo.addGoal(name,moneyToMinor(target),currency,rate.toDoubleOrNull()?:1.0,d);"Meta creada"}
 fun contribute(id:Long,amount:String)=op{repo.contribute(id,moneyToMinor(amount));"Aporte registrado"}
 private fun op(block:suspend()->String){viewModelScope.launch{runCatching{block()}.onSuccess{_notice.emit(it)}.onFailure{_notice.emit(it.message?:"Revisa los datos")}}}
 companion object{fun factory(repo:FinanceRepository)=object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=FinanceViewModel(repo) as T}}
}
