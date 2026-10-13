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

val Gold=Color(0xFFC8A84C);val Green=Color(0xFF55A57A);val Red=Color(0xFFD26F6F);val Amber=Color(0xFFD1A452);val Muted=Color(0xFF9A9A95)
private val freqs=listOf("SEMANAL","CATORCENAL","QUINCENAL","MENSUAL")
private fun fmt(minor:Long,currency:String):String=runCatching{NumberFormat.getCurrencyInstance(Locale("es","MX")).apply{this.currency=Currency.getInstance(currency);minimumFractionDigits=2;maximumFractionDigits=2}.format(minor/100.0)}.getOrDefault("$${"%.2f".format(minor/100.0)} $currency")

@Composable fun FinanceApp(vm:FinanceViewModel){
 val profile by vm.profile.collectAsState();val fixed by vm.fixed.collectAsState();val cards by vm.cards.collectAsState();val goals by vm.goals.collectAsState();val expenses by vm.expenses.collectAsState()
 var tab by remember{mutableIntStateOf(0)};var form by remember{mutableStateOf("")};var targetId by remember{mutableLongStateOf(0)};var notice by remember{mutableStateOf("")}
 LaunchedEffect(Unit){vm.notice.collect{notice=it}}
 val budget=remember(profile,fixed,cards,goals,expenses){calculateBudget(profile?.incomeMinor?:0,profile?.payFrequency?:"QUINCENAL",fixed,cards,goals,expenses)}
 Scaffold(containerColor=Color(0xFF0A0B0D),bottomBar={NavigationBar(containerColor=Color(0xFF101214)){listOf("Inicio","Gastos","Fijos","Tarjetas","Metas").forEachIndexed{index,label->NavigationBarItem(selected=tab==index,onClick={tab=index},icon={Text(listOf("⌂","−","↻","▣","◎")[index],fontSize=17.sp)},label={Text(label,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=Gold,selectedTextColor=Gold,indicatorColor=Color(0xFF24221A),unselectedIconColor=Muted,unselectedTextColor=Muted))}}}){padding->
  Column(Modifier.fillMaxSize().padding(padding)) {
   Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Column{Text("CONTROL FINANCIERO",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp);Text(listOf("Resumen","Gastos del periodo","Gastos recurrentes","Tarjetas","Metas")[tab],fontSize=22.sp,fontWeight=FontWeight.SemiBold)};TextButton(onClick={form="income"}){Text("${profile?.currency?:"MXN"}  ✎",color=Gold)}}
   when(tab){0->HomeScreen(profile,budget,fixed,cards,goals,onEditIncome={form="income"},onNavigate={tab=it},onExpense={form="expense"});1->ExpensesScreen(profile,expenses,onAdd={form="expense"});2->FixedScreen(profile,fixed,onAdd={form="fixed"},onDelete=vm::deleteFixed);3->CardsScreen(cards,onAdd={form="card"},onPay={targetId=it;form="pay"});else->GoalsScreen(goals,onAdd={form="goal"},onContribute={targetId=it;form="contribute"})}
  }
 }
 if(form.isNotBlank())FinanceDialog(form,profile?.currency?:"MXN",onClose={form=""}){v->when(form){
  "income"->vm.saveIncome(v["amount"].orEmpty(),v["currency"].orEmpty(),v["frequency"].orEmpty())
  "fixed"->vm.addFixed(v["name"].orEmpty(),v["amount"].orEmpty(),v["currency"].orEmpty(),v["rate"].orEmpty(),v["frequency"].orEmpty(),v["due"].orEmpty())
  "expense"->vm.spend(v["quick"].orEmpty(),v["currency"].orEmpty(),v["rate"].orEmpty(),cards.firstOrNull()?.id,v["category"].orEmpty())
  "card"->vm.addCard(v["name"].orEmpty(),v["limit"].orEmpty(),v["cut"].orEmpty(),v["due"].orEmpty(),v["salary"]=="true",v["payment"].orEmpty(),v["currency"].orEmpty(),v["rate"].orEmpty())
  "pay"->vm.payCard(targetId,v["amount"].orEmpty())
  "goal"->vm.addGoal(v["name"].orEmpty(),v["amount"].orEmpty(),v["currency"].orEmpty(),v["rate"].orEmpty(),v["deadline"].orEmpty())
  "contribute"->vm.contribute(targetId,v["amount"].orEmpty())
 };form=""}
 if(notice.isNotBlank())AlertDialog(onDismissRequest={notice=""},confirmButton={TextButton(onClick={notice=""}){Text("Entendido",color=Gold)}},text={Text(notice)})
}

@Composable private fun HomeScreen(p:Profile?,b:BudgetResult,fixed:List<FixedExpense>,cards:List<CreditCard>,goals:List<SavingsGoal>,onEditIncome:()->Unit,onNavigate:(Int)->Unit,onExpense:()->Unit){
 LazyColumn(contentPadding=PaddingValues(horizontal=18.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  item{Surface(shape=RoundedCornerShape(22.dp),color=Color(0xFF171816),modifier=Modifier.fillMaxWidth().clickable(onClick=onEditIncome)){Column(Modifier.padding(20.dp)){Text("SALDO LIBRE DE ESTE PERIODO",color=Muted,fontSize=11.sp,fontWeight=FontWeight.Bold);Spacer(Modifier.height(8.dp));Text(fmt(b.free,p?.currency?:"MXN"),color=if(b.free<0)Red else Gold,fontSize=34.sp,fontWeight=FontWeight.Bold);Text(if(p==null)"Configura tu ingreso para empezar" else "Ingreso ${freqLabel(p.payFrequency)} · ${p.currency}",color=Muted,fontSize=13.sp)}}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Ingreso",fmt(b.income,p?.currency?:"MXN"),Modifier.weight(1f));Metric("Apartar",fmt(b.reserve,p?.currency?:"MXN"),Modifier.weight(1f))}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Ahorro sugerido",fmt(b.goalReserve,p?.currency?:"MXN"),Modifier.weight(1f));Metric("Gastado",fmt(b.spent,p?.currency?:"MXN"),Modifier.weight(1f))}}
  item{if(b.free<=0&&p!=null)AlertCard("⚠️ Saldo libre agotado o negativo. Revisa gastos y apartados.",Red)else if(p!=null&&b.free<=b.income/5)AlertCard("⚠️ Tu saldo libre está por debajo del 20% del ingreso del periodo.",Amber)}
  item{Button(onClick=onExpense,modifier=Modifier.fillMaxWidth().height(54.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black),shape=RoundedCornerShape(16.dp)){Text("＋  Registrar gasto",fontSize=16.sp,fontWeight=FontWeight.Bold)}}
  item{Text("RESUMEN",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)}
  item{Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Metric("Gastos fijos",fixed.count{it.enabled}.toString(),Modifier.weight(1f));Metric("Tarjetas",cards.size.toString(),Modifier.weight(1f));Metric("Metas",goals.count{it.savedMinor<it.targetMinor}.toString(),Modifier.weight(1f))}}
  item{Text("El apartado se estima usando las periodicidades configuradas. Puedes editarlo cuando cambien tus fechas o montos.",color=Muted,fontSize=12.sp)}
 }
}
@Composable private fun Metric(title:String,value:String,modifier:Modifier=Modifier){Surface(modifier=modifier,shape=RoundedCornerShape(16.dp),color=Color(0xFF15171A)){Column(Modifier.padding(14.dp)){Text(title,color=Muted,fontSize=11.sp);Spacer(Modifier.height(4.dp));Text(value,color=Color.White,fontSize=16.sp,fontWeight=FontWeight.SemiBold)}}}
@Composable private fun AlertCard(text:String,color:Color){Surface(shape=RoundedCornerShape(14.dp),color=color.copy(alpha=.15f),modifier=Modifier.fillMaxWidth()){Text(text,Modifier.padding(14.dp),color=color,fontSize=13.sp)}}

@Composable private fun ExpensesScreen(p:Profile?,expenses:List<Expense>,onAdd:()->Unit){Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){Text("Registro rápido",fontWeight=FontWeight.SemiBold,fontSize=18.sp);Text("Ejemplos: Gasto $120 tacos · Tarjeta $500 comida",color=Muted,fontSize=12.sp);Spacer(Modifier.height(12.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().height(52.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black)){Text("＋  Nuevo gasto")};Spacer(Modifier.height(14.dp));Text("MOVIMIENTOS DE ESTE PERIODO",color=Gold,fontSize=11.sp,fontWeight=FontWeight.Bold);LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=10.dp)){items(expenses){e->Surface(shape=RoundedCornerShape(14.dp),color=Color(0xFF15171A)){Row(Modifier.fillMaxWidth().padding(14.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(e.concept,color=Color.White,fontWeight=FontWeight.Medium);Text("${e.category} · ${if(e.cardId!=null)"Tarjeta" else "Efectivo"}",color=Muted,fontSize=12.sp)};Text("−${fmt(e.amountMinor,e.currency)}",color=Red,fontWeight=FontWeight.SemiBold)}}}};if(expenses.isEmpty())Text("Aún no hay gastos registrados.",color=Muted,modifier=Modifier.padding(top=20.dp))}}

@Composable private fun FixedScreen(p:Profile?,items:List<FixedExpense>,onAdd:()->Unit,onDelete:(Long)->Unit){Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){Text("Tu reserva por sueldo considera la periodicidad de cada cobro.",color=Muted,fontSize=12.sp);Spacer(Modifier.height(10.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().height(50.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black)){Text("＋  Agregar gasto recurrente")};LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp),contentPadding=PaddingValues(vertical=14.dp)){items(items){x->val per=(toBaseMinor(x.amountMinor,x.exchangeRate)*chargePeriodsPerYear(x.frequency)/payPeriodsPerYear(p?.payFrequency?:"QUINCENAL")).toLong();Surface(shape=RoundedCornerShape(14.dp),color=Color(0xFF15171A)){Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(x.title,color=Color.White,fontWeight=FontWeight.SemiBold);Text("${fmt(x.amountMinor,x.currency)} · ${freqLabel(x.frequency)} · ${x.dueLabel}",color=Muted,fontSize=11.sp);Text("Apartar ${fmt(per,p?.currency?:"MXN")} por pago",color=Gold,fontSize=12.sp)};TextButton(onClick={onDelete(x.id)}){Text("Quitar",color=Red)}}}}};if(items.isEmpty())Text("Agrega renta, servicios o cualquier compromiso periódico.",color=Muted)}}

@Composable private fun CardsScreen(cards:List<CreditCard>,onAdd:()->Unit,onPay:(Long)->Unit){Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){Text("Sin números de tarjeta, CVV ni NIP.",color=Muted,fontSize=12.sp);Spacer(Modifier.height(10.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().height(50.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black)){Text("＋  Agregar tarjeta")};LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=14.dp)){items(cards){c->Surface(shape=RoundedCornerShape(18.dp),color=Color(0xFF171816)){Column(Modifier.fillMaxWidth().padding(16.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Column{Text(c.name,color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=17.sp);Text("Corte ${c.cutDay} · Paga antes del ${c.dueDay}",color=Muted,fontSize=12.sp)};Text("${((c.usedMinor*100)/c.limitMinor).coerceIn(0,100)}%",color=Gold,fontWeight=FontWeight.Bold)};Spacer(Modifier.height(12.dp));LinearProgressIndicator(progress={if(c.limitMinor>0)(c.usedMinor.toFloat()/c.limitMinor).coerceIn(0f,1f)else 0f},modifier=Modifier.fillMaxWidth(),color=if(c.usedMinor*100>=c.limitMinor*80)Red else Gold,trackColor=Color(0xFF303236));Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Usado ${fmt(c.usedMinor,c.currency)}",color=Color.White,fontSize=13.sp);Text("Disponible ${fmt((c.limitMinor-c.usedMinor).coerceAtLeast(0),c.currency)}",color=Green,fontSize=13.sp)};Text(if(c.salaryFunded)"Pago mensual reservado desde tu ingreso: ${fmt(c.expectedPaymentMinor,c.currency)}" else "Pago desde presupuesto aparte",color=Muted,fontSize=12.sp);TextButton(onClick={onPay(c.id)}){Text("Registrar abono",color=Gold)}}}}};if(cards.isEmpty())Text("Agrega un alias, límite, fechas y si el pago sale del salario.",color=Muted)}}

@Composable private fun GoalsScreen(goals:List<SavingsGoal>,onAdd:()->Unit,onContribute:(Long)->Unit){Column(Modifier.fillMaxSize().padding(horizontal=18.dp)){Text("Aportes estimados por periodo hasta tu fecha límite.",color=Muted,fontSize=12.sp);Spacer(Modifier.height(10.dp));Button(onClick=onAdd,modifier=Modifier.fillMaxWidth().height(50.dp),colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black)){Text("＋  Nueva meta")};LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=14.dp)){items(goals){g->Surface(shape=RoundedCornerShape(16.dp),color=Color(0xFF15171A)){Column(Modifier.fillMaxWidth().padding(15.dp)){Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(g.name,color=Color.White,fontWeight=FontWeight.SemiBold);Text(LocalDate.ofEpochDay(g.deadlineEpochDay).toString(),color=Muted,fontSize=11.sp)};Spacer(Modifier.height(8.dp));LinearProgressIndicator(progress={if(g.targetMinor>0)(g.savedMinor.toFloat()/g.targetMinor).coerceIn(0f,1f)else 0f},modifier=Modifier.fillMaxWidth(),color=Green,trackColor=Color(0xFF303236));Text("${fmt(g.savedMinor,g.currency)} de ${fmt(g.targetMinor,g.currency)}",color=Color.White,fontSize=13.sp,modifier=Modifier.padding(top=6.dp));TextButton(onClick={onContribute(g.id)}){Text("＋ Aportar",color=Gold)}}}}};if(goals.isEmpty())Text("Crea un fondo, un viaje o cualquier objetivo de ahorro.",color=Muted)}}

private fun freqLabel(s:String)=when(s){"SEMANAL"->"Semanal";"CATORCENAL"->"Catorcenal";"QUINCENAL"->"Quincenal";else->"Mensual"}
