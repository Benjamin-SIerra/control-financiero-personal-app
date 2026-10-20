package com.benjamin.controlfinanciero.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.benjamin.controlfinanciero.data.*
import com.benjamin.controlfinanciero.domain.*
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val Gold=Color(0xFFC8A84C);val Green=Color(0xFF55A57A);val Red=Color(0xFFD26F6F);val Amber=Color(0xFFD1A452);val Muted=Color(0xFF9A9A95)
private val freqs=listOf("SEMANAL","CATORCENAL","QUINCENAL","MENSUAL")
private fun fmt(minor:Long,currency:String):String=runCatching{NumberFormat.getCurrencyInstance(Locale("es","MX")).apply{this.currency=Currency.getInstance(currency);minimumFractionDigits=2;maximumFractionDigits=2}.format(minor/100.0)}.getOrDefault("$${"%.2f".format(minor/100.0)} $currency")
private fun dateLabel(epochDay:Long)=LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

@Composable fun FinanceApp(vm:FinanceViewModel){
 val profile by vm.profile.collectAsState();val periods by vm.incomePeriods.collectAsState();val activePeriod by vm.activeIncomePeriod.collectAsState();val balanceState by vm.periodBalance.collectAsState();val today by vm.todayEpochDay.collectAsState();val fixed by vm.fixed.collectAsState();val cards by vm.cards.collectAsState();val goals by vm.goals.collectAsState();val expenses by vm.expenses.collectAsState()
 var tab by remember{mutableIntStateOf(0)};var incomeScreen by remember{mutableStateOf(false)};var form by remember{mutableStateOf("")};var targetId by remember{mutableLongStateOf(0)};var notice by remember{mutableStateOf("")};var editingPeriod by remember{mutableStateOf<IncomePeriod?>(null)};var editingGoal by remember{mutableStateOf<SavingsGoal?>(null)};var pendingDelete by remember{mutableStateOf<IncomePeriod?>(null)};var pendingGoalAction by remember{mutableStateOf<Pair<SavingsGoal,String>?>(null)};var defaultIncomeStart by remember{mutableLongStateOf(0)}
 LaunchedEffect(Unit){vm.notice.collect{notice=it}}
 val budget=remember(activePeriod,profile,fixed,cards,goals,expenses,balanceState){calculateBudget(activePeriod?.incomeMinor?:0,activePeriod?.frequency?:profile?.payFrequency?:"QUINCENAL",fixed,cards,goals,expenses,carryoverMinor=balanceState?.carryoverMinor?:0,manualAdjustmentMinor=balanceState?.manualAdjustmentMinor?:0)}
 Scaffold(containerColor=Color(0xFF0A0B0D),bottomBar={NavigationBar(containerColor=Color(0xFF101214)){listOf("Inicio","Gastos","Fijos","Tarjetas","Metas").forEachIndexed{index,label->NavigationBarItem(selected=tab==index,onClick={tab=index;incomeScreen=false},icon={Text(listOf("⌂","−","↻","▣","◎")[index],fontSize=17.sp)},label={Text(label,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Gold,selectedTextColor=Gold,indicatorColor=Color(0xFF24221A),unselectedIconColor=Muted,unselectedTextColor=Muted))}}}){padding->
  Column(Modifier.fillMaxSize().padding(padding)) {
   Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Column{Text("CONTROL FINANCIERO",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp);Text(if(incomeScreen)"Ingresos / Periodos" else listOf("Resumen","Gastos del periodo","Gastos recurrentes","Tarjetas","Metas")[tab],fontSize=22.sp,fontWeight=FontWeight.SemiBold)};TextButton(onClick={if(incomeScreen)incomeScreen=false else incomeScreen=true}){Text(if(incomeScreen)"Volver" else "Ingresos",color=Gold)}}
   if(incomeScreen)IncomePeriodsScreen(periods,today,onAdd={defaultIncomeStart=periods.maxOfOrNull{it.endEpochDay}?.plus(1)?:today;editingPeriod=null;form="income_period"},onEdit={editingPeriod=it;form="income_period"},onDelete={pendingDelete=it}) else when(tab){0->HomeScreen(profile,activePeriod,budget,fixed,cards,goals,onEditIncome={editingPeriod=activePeriod;form="income_period"},onEditBalance={form="free_balance"},onNavigate={tab=it},onExpense={form="expense"});1->ExpensesScreen(profile,expenses,onAdd={form="expense"});2->FixedScreen(profile,fixed,onAdd={form="fixed"},onDelete=vm::deleteFixed);3->CardsScreen(cards,onAdd={form="card"},onPay={targetId=it;form="pay"});else->GoalsScreen(goals,profile?.currency?:"MXN",onAdd={editingGoal=null;form="goal"},onEdit={editingGoal=it;form="goal"},onContribute={editingGoal=it;targetId=it.id;form="contribute"},onAction={goal,action->pendingGoalAction=goal to action})}
  }
 }
 if(form.isNotBlank())FinanceDialog(form,profile?.currency?:"MXN",onClose={form="";editingPeriod=null;editingGoal=null},onSave={v->when(form){
  "income_period"->vm.saveIncomePeriod(v["id"].orEmpty(),v["start"].orEmpty(),v["end"].orEmpty(),v["amount"].orEmpty(),v["currency"].orEmpty(),v["frequency"].orEmpty())
  "fixed"->vm.addFixed(v["name"].orEmpty(),v["amount"].orEmpty(),v["currency"].orEmpty(),v["rate"].orEmpty(),v["frequency"].orEmpty(),v["due"].orEmpty())
  "expense"->vm.spend(v["category"].orEmpty(),v["amount"].orEmpty(),v["currency"].orEmpty(),v["rate"].orEmpty(),if(v["isCard"]=="true")cards.firstOrNull()?.id else null,v["isCard"]=="true")
  "free_balance"->vm.setCurrentFreeBalance(v["amount"].orEmpty())
  "card"->vm.addCard(v["name"].orEmpty(),v["limit"].orEmpty(),v["cut"].orEmpty(),v["due"].orEmpty(),v["salary"]=="true",v["payment"].orEmpty(),v["currency"].orEmpty(),v["rate"].orEmpty())
  "pay"->vm.payCard(targetId,v["amount"].orEmpty())
  "goal"->{val id=v["id"].orEmpty().toLongOrNull();if(id==null)vm.addGoal(v["name"].orEmpty(),v["amount"].orEmpty(),v["currency"].orEmpty(),v["rate"].orEmpty(),v["deadline"].orEmpty())else vm.updateGoal(id,v["name"].orEmpty(),v["amount"].orEmpty(),v["currency"].orEmpty(),v["rate"].orEmpty(),v["deadline"].orEmpty())}
  "contribute"->vm.contribute(targetId,v["amount"].orEmpty())
 };form="";editingPeriod=null;editingGoal=null},incomePeriod=editingPeriod,goal=editingGoal,defaultStartEpochDay=defaultIncomeStart.takeIf{it>0},defaultFrequency=profile?.payFrequency?:"QUINCENAL",currentBalanceMinor=budget.free,hasCards=cards.isNotEmpty(),firstCardName=cards.firstOrNull()?.name)
 balanceState?.takeIf{it.pendingCarryoverMinor>0&&!it.rolloverResolved}?.let{pendingBalance->
  val weekly=activePeriod?.frequency=="SEMANAL"
  val question=if(weekly)"¿Deseas sumar el remanente de la semana anterior (${fmt(pendingBalance.pendingCarryoverMinor,activePeriod?.currency?:"MXN")}) al saldo disponible de esta nueva semana?" else "¿Deseas sumar el remanente del periodo anterior (${fmt(pendingBalance.pendingCarryoverMinor,activePeriod?.currency?:"MXN")}) al saldo disponible de este nuevo periodo?"
  AlertDialog(onDismissRequest={},title={Text("Remanente disponible")},text={Text(question)},confirmButton={TextButton(onClick={vm.decideCarryover(true)}){Text("Sí, sumar",color=Gold)}},dismissButton={TextButton(onClick={vm.decideCarryover(false)}){Text("No, gracias",color=Muted)}})
 }
 if(pendingDelete!=null)AlertDialog(onDismissRequest={pendingDelete=null},title={Text("Eliminar periodo")},text={Text("Se eliminará el ingreso de ${dateLabel(pendingDelete!!.startEpochDay)} a ${dateLabel(pendingDelete!!.endEpochDay)}. Esta acción no se puede deshacer.")},confirmButton={TextButton(onClick={pendingDelete?.let{vm.deleteIncomePeriod(it.id)};pendingDelete=null}){Text("Eliminar",color=Red)}},dismissButton={TextButton(onClick={pendingDelete=null}){Text("Cancelar",color=Muted)}})
 pendingGoalAction?.let{(g,action)->
  val actionLabel=when(action){"COMPLETED"->"Completar";"CANCELLED"->"Cancelar meta";else->"Eliminar"}
  AlertDialog(onDismissRequest={pendingGoalAction=null},title={Text("$actionLabel · ${g.name}")},text={Text(when(action){"COMPLETED"->"La meta quedará en el historial y dejará de apartar dinero desde el siguiente recálculo.";"CANCELLED"->"La meta se conservará como cancelada, pero su apartado recurrente se liberará.";else->"La meta se eliminará definitivamente. Su apartado recurrente se liberará."})},confirmButton={TextButton(onClick={when(action){"COMPLETED"->vm.setGoalStatus(g.id,"COMPLETED");"CANCELLED"->vm.setGoalStatus(g.id,"CANCELLED");else->vm.deleteGoal(g.id)};pendingGoalAction=null}){Text(actionLabel,color=Red)}},dismissButton={TextButton(onClick={pendingGoalAction=null}){Text("Volver",color=Muted)}})
 }
 if(notice.isNotBlank())AlertDialog(onDismissRequest={notice=""},confirmButton={TextButton(onClick={notice=""}){Text("Entendido",color=Gold)}},text={Text(notice)})
}

@Composable private fun HomeScreen(p:Profile?,period:IncomePeriod?,b:BudgetResult,fixed:List<FixedExpense>,cards:List<CreditCard>,goals:List<SavingsGoal>,onEditIncome:()->Unit,onEditBalance:()->Unit,onNavigate:(Int)->Unit,onExpense:()->Unit){
 val currency=period?.currency?:p?.currency?:"MXN"
 LazyColumn(contentPadding=PaddingValues(horizontal=18.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Surface(shape=RoundedCornerShape(22.dp),color=Color(0xFF171816),modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(20.dp)){Text("SALDO LIBRE DE ESTE PERIODO",color=Muted,fontSize=11.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text(fmt(b.free,currency),color=if(b.free<0)Red else Gold,fontSize=34.sp,fontWeight=FontWeight.Bold);Text(if(period==null)"Preparando periodo de ingreso" else "${freqLabel(period.frequency)} · ${dateLabel(period.startEpochDay)}–${dateLabel(period.endEpochDay)}",color=Muted,fontSize=13.sp);if(period?.incomeMinor==0L)Text("Ingreso pendiente de registrar",color=Amber,fontSize=12.sp);Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick=onEditBalance){Text("Ajustar saldo",color=Gold)};TextButton(onClick=onEditIncome){Text("Editar ingreso",color=Muted)}}}}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Ingreso",if(period?.incomeMinor==0L)"Por registrar" else fmt(b.income,currency),Modifier.weight(1f));Metric("Apartar",fmt(b.reserve,currency),Modifier.weight(1f))}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Ahorro sugerido",fmt(b.goalReserve,p?.currency?:"MXN"),Modifier.weight(1f));Metric("Gastado",fmt(b.spent,p?.currency?:"MXN"),Modifier.weight(1f))}}
  item{if(b.free<=0&&period?.incomeMinor!=0L)AlertCard("⚠️ Saldo libre agotado o negativo. Revisa gastos y apartados.",Red)else if(period!=null&&period.incomeMinor>0&&b.free<=b.income/5)AlertCard("⚠️ Tu saldo libre está por debajo del 20% del ingreso del periodo.",Amber)}
  item{Button(onClick=onExpense,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black),shape=RoundedCornerShape(16.dp)){Text("＋  Registrar gasto",fontSize=16.sp,fontWeight=FontWeight.Bold)}}
  item{Text("RESUMEN",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Gastos fijos",fixed.count{it.enabled}.toString(),Modifier.weight(1f));Metric("Tarjetas",cards.size.toString(),Modifier.weight(1f));Metric("Metas",goals.count{it.status=="ACTIVE"}.toString(),Modifier.weight(1f))}}
  item{Text("El apartado se estima usando las periodicidades configuradas. Puedes editarlo cuando cambien tus fechas o montos.",color=Muted,fontSize=12.sp)}
 }
}
@Composable private fun IncomePeriodsScreen(periods:List<IncomePeriod>,todayEpochDay:Long,onAdd:()->Unit,onEdit:(IncomePeriod)->Unit,onDelete:(IncomePeriod)->Unit){
 Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){
  Text("Cada periodo conserva su ingreso real. Al terminar su rango pasa a histórico y se abre otro periodo sin copiar el monto anterior.",color=Muted,fontSize=12.sp)
  Spacer(Modifier.height(10.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().height(50.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black),shape=RoundedCornerShape(14.dp)){Text("＋  Nuevo periodo de ingreso")}
  LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(vertical=14.dp)){
   items(periods,key={it.id}){period->
    val status=incomePeriodStatus(period.startEpochDay,period.endEpochDay,todayEpochDay)
    val (label,color)=when(status){IncomePeriodStatus.ACTIVE->"ACTUAL" to Green;IncomePeriodStatus.HISTORICAL->"HISTÓRICO" to Muted;IncomePeriodStatus.UPCOMING->"PROGRAMADO" to Amber}
    Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFF15171A)){
     Column(Modifier.fillMaxWidth().padding(15.dp)){
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("${dateLabel(period.startEpochDay)} – ${dateLabel(period.endEpochDay)}",color=Color.White,fontWeight=FontWeight.SemiBold);Text(label,color=color,fontSize=10.sp,fontWeight=FontWeight.Bold)}
      Spacer(Modifier.height(5.dp));Text("${freqLabel(period.frequency)} · ${period.currency}",color=Muted,fontSize=12.sp)
      Text(if(period.incomeMinor==0L)"Ingreso pendiente de registrar" else "Ingreso real: ${fmt(period.incomeMinor,period.currency)}",color=if(period.incomeMinor==0L)Amber else Gold,fontSize=14.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=5.dp))
      Row(horizontalArrangement=Arrangement.End,modifier=Modifier.fillMaxWidth()){TextButton(onClick={onEdit(period)}){Text("Editar",color=Gold)};TextButton(onClick={onDelete(period)}){Text("Eliminar",color=Red)}}
     }
    }
   }
   if(periods.isEmpty())item{Text("Aún no hay periodos de ingreso.",color=Muted,modifier=Modifier.padding(top=18.dp))}
  }
 }
}

@Composable private fun Metric(title:String,value:String,modifier:Modifier=Modifier){Surface(modifier=modifier,shape=RoundedCornerShape(16.dp),color=Color(0xFF15171A)){Column(Modifier.padding(14.dp)){Text(title,color=Muted,fontSize=11.sp);Spacer(Modifier.height(4.dp));Text(value,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.SemiBold)}}}
@Composable private fun AlertCard(text:String,color:Color){Surface(shape=RoundedCornerShape(14.dp),color=color.copy(alpha=.15f),modifier=Modifier.fillMaxWidth()){Text(text,Modifier.padding(14.dp),color=color,fontSize=13.sp)}}

@Composable private fun ExpensesScreen(p:Profile?,expenses:List<Expense>,onAdd:()->Unit){Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){Text("Registro estructurado",fontWeight=FontWeight.SemiBold,fontSize=18.sp);Text("Elige una categoría y captura un monto numérico mayor a cero.",color=Muted,fontSize=12.sp);Spacer(Modifier.height(12.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black)){Text("＋  Nuevo gasto")};Spacer(Modifier.height(14.dp));Text("MOVIMIENTOS DE ESTE PERIODO",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold);LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=10.dp)){items(expenses){e->Surface(shape=RoundedCornerShape(14.dp),color=Color(0xFF15171A)){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(e.concept,color=Color.White,fontWeight=FontWeight.Medium);Text("${e.category} · ${if(e.cardId!=null)"Tarjeta" else "Efectivo"}",color=Muted,fontSize=12.sp);if(e.currency!= (p?.currency?:"MXN"))Text("≈ ${fmt(toBaseMinor(e.amountMinor,e.exchangeRate),p?.currency?:"MXN")} · tasa ${e.exchangeRate}",color=Muted,fontSize=11.sp)};Text("−${fmt(e.amountMinor,e.currency)}",color=Red,fontWeight=FontWeight.SemiBold)}}}};if(expenses.isEmpty())Text("Aún no hay gastos registrados.",color=Muted,modifier=Modifier.padding(top=20.dp))}}

@Composable private fun FixedScreen(p:Profile?,items:List<FixedExpense>,onAdd:()->Unit,onDelete:(Long)->Unit){Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){Text("Tu reserva por sueldo considera la periodicidad de cada cobro.",color=Muted,fontSize=12.sp);Spacer(Modifier.height(10.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().height(50.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black)){Text("＋  Agregar gasto recurrente")};LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(vertical=14.dp)){items(items){x->val monthly=fixedExpenseMonthlyEquivalent(x);Surface(shape=RoundedCornerShape(14.dp),color=Color(0xFF15171A)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(x.title,color=Color.White,fontWeight=FontWeight.SemiBold);Text("${freqLabel(x.frequency)} · ${x.dueLabel}",color=Muted,fontSize=11.sp);Text("Por pago (${freqLabel(x.frequency)}): ${fmt(x.amountMinor,x.currency)}",color=Gold,fontSize=12.sp);Text("Acumulado mensual estimado: ${fmt(monthly,p?.currency?:"MXN")}",color=Muted,fontSize=11.sp)};TextButton(onClick={onDelete(x.id)}){Text("Quitar",color=Red)}}}}};if(items.isEmpty())Text("Agrega renta, servicios o cualquier compromiso periódico.",color=Muted)}}

@Composable private fun CardsScreen(cards:List<CreditCard>,onAdd:()->Unit,onPay:(Long)->Unit){Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){Text("Sin números de tarjeta, CVV ni NIP.",color=Muted,fontSize=12.sp);Spacer(Modifier.height(10.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().height(50.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black)){Text("＋  Agregar tarjeta")};LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=14.dp)){items(cards){c->Surface(shape=RoundedCornerShape(18.dp),color=Color(0xFF171816)){Column(Modifier.fillMaxWidth().padding(16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(c.name,color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=17.sp);Text("Corte ${c.cutDay} · Paga antes del ${c.dueDay}",color=Muted,fontSize=12.sp)};Text("${((c.usedMinor*100)/c.limitMinor).coerceIn(0,100)}%",color=Gold,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(12.dp));LinearProgressIndicator(progress={if(c.limitMinor>0)(c.usedMinor.toFloat()/c.limitMinor).coerceIn(0f,1f)else 0f},modifier=Modifier.fillMaxWidth(),color=if(c.usedMinor*100>=c.limitMinor*80)Red else Gold,trackColor=Color(0xFF303236));Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Usado ${fmt(c.usedMinor,c.currency)}",color=Color.White,fontSize=13.sp);Text("Disponible ${fmt((c.limitMinor-c.usedMinor).coerceAtLeast(0),c.currency)}",color=Green,fontSize=13.sp)};Text(if(c.salaryFunded)"Pago mensual reservado desde tu ingreso: ${fmt(c.expectedPaymentMinor,c.currency)}" else "Pago desde presupuesto aparte",color=Muted,fontSize=12.sp);TextButton(onClick={onPay(c.id)}){Text("Registrar abono",color=Gold)}}}}};if(cards.isEmpty())Text("Agrega un alias, límite, fechas y si el pago sale del salario.",color=Muted)}}

@Composable private fun GoalsScreen(goals:List<SavingsGoal>,baseCurrency:String,onAdd:()->Unit,onEdit:(SavingsGoal)->Unit,onContribute:(SavingsGoal)->Unit,onAction:(SavingsGoal,String)->Unit){
 Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){
  Text("Los apartados activos se calculan en moneda base con la tasa guardada. Cancelar o completar libera el saldo libre.",color=Muted,fontSize=12.sp)
  Spacer(Modifier.height(10.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().height(50.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black)){Text("＋  Nueva meta")}
  LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=14.dp)){
   items(goals,key={it.id}){g->
    val active=g.status=="ACTIVE"
    Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFF15171A)){
     Column(Modifier.fillMaxWidth().padding(15.dp)){
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(g.name,color=Color.White,fontWeight=FontWeight.SemiBold);Text(when(g.status){"COMPLETED"->"COMPLETADA";"CANCELLED"->"CANCELADA";else->LocalDate.ofEpochDay(g.deadlineEpochDay).toString()},color=if(active)Muted else Green,fontSize=11.sp)}
      Spacer(Modifier.height(8.dp));LinearProgressIndicator(progress={if(g.targetMinor>0)(g.savedMinor.toFloat()/g.targetMinor).coerceIn(0f,1f)else 0f},modifier=Modifier.fillMaxWidth(),color=Green,trackColor=Color(0xFF303236))
      Text("${fmt(g.savedMinor,g.currency)} de ${fmt(g.targetMinor,g.currency)} · ${g.currency}",color=Color.White,fontSize=13.sp,modifier=Modifier.padding(top=6.dp))
      Text("Tasa usada: 1 ${g.currency} = ${g.exchangeRate} $baseCurrency",color=Muted,fontSize=11.sp)
      Text("Equivalente objetivo estimado: ${fmt(toBaseMinor(g.targetMinor,g.exchangeRate),baseCurrency)}",color=Muted,fontSize=11.sp)
      if(active){
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){TextButton(onClick={onEdit(g)}){Text("Editar",color=Gold)};TextButton(onClick={onContribute(g)}){Text("＋ Aportar",color=Gold)}}
       Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){TextButton(onClick={onAction(g,"COMPLETED")}){Text("Completar",color=Green)};TextButton(onClick={onAction(g,"CANCELLED")}){Text("Cancelar",color=Amber)}}
      }
      Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){TextButton(onClick={onAction(g,"DELETE")}){Text("Eliminar",color=Red)}}
     }
    }
   }
   if(goals.isEmpty())item{Text("Crea un fondo, un viaje o cualquier objetivo de ahorro.",color=Muted)}
  }
 }
}

private fun freqLabel(s:String)=when(s){"SEMANAL"->"Semanal";"CATORCENAL"->"Catorcenal";"QUINCENAL"->"Quincenal";else->"Mensual"}
