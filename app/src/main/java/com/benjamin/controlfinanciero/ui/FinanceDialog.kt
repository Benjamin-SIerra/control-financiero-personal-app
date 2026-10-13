package com.benjamin.controlfinanciero.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.ZoneOffset
import com.benjamin.controlfinanciero.data.IncomePeriod
import com.benjamin.controlfinanciero.data.SavingsGoal
import com.benjamin.controlfinanciero.domain.defaultIncomePeriodEnd
import com.benjamin.controlfinanciero.domain.selectedDateMillisToIsoDate

@Composable fun FinanceDialog(kind:String,baseCurrency:String,onClose:()->Unit,onSave:(Map<String,String>)->Unit,incomePeriod:IncomePeriod?=null,goal:SavingsGoal?=null,defaultStartEpochDay:Long?=null,defaultFrequency:String="QUINCENAL"){
 val v=remember(kind,incomePeriod?.id,goal?.id){mutableStateMapOf<String,String>()};var salary by remember(kind){mutableStateOf(true)};var category by remember(kind){mutableStateOf("Otros")}
 LaunchedEffect(kind,baseCurrency,incomePeriod?.id,goal?.id,defaultStartEpochDay,defaultFrequency){
  v.clear();v["currency"]=goal?.currency?:incomePeriod?.currency?:baseCurrency;v["rate"]=goal?.exchangeRate?.toString()?:if(v["currency"]==baseCurrency)"1" else "";v["frequency"]=incomePeriod?.frequency?:defaultFrequency;v["deadline"]=goal?.deadlineEpochDay?.let{LocalDate.ofEpochDay(it).toString()}?:LocalDate.now().plusMonths(3).toString();v["category"]="Otros"
  if(kind=="goal") {v["id"]=goal?.id?.toString().orEmpty();v["name"]=goal?.name.orEmpty();v["amount"]=goal?.targetMinor?.let{String.format(java.util.Locale.US,"%.2f",it/100.0)}.orEmpty()}
  if(kind=="income_period"){
   val start=incomePeriod?.startEpochDay?.let(LocalDate::ofEpochDay)?:defaultStartEpochDay?.let(LocalDate::ofEpochDay)?:LocalDate.now()
   val end=incomePeriod?.endEpochDay?.let(LocalDate::ofEpochDay)?:LocalDate.ofEpochDay(defaultIncomePeriodEnd(start.toEpochDay(),v["frequency"]!!))
   v["id"]=incomePeriod?.id?.toString().orEmpty();v["amount"]=incomePeriod?.incomeMinor?.let{String.format(java.util.Locale.US,"%.2f",it/100.0)}?:"";v["start"]=start.toString();v["end"]=end.toString()
  }
 }
 val title=when(kind){"income_period"->if(incomePeriod==null)"Nuevo periodo de ingreso" else "Editar periodo de ingreso";"fixed"->"Gasto recurrente";"expense"->"Registrar gasto";"card"->"Nueva tarjeta";"pay"->"Registrar abono";"goal"->if(goal==null)"Nueva meta" else "Editar meta";else->"Aportación a meta"}
 AlertDialog(onDismissRequest=onClose,title={Text(title,fontWeight=FontWeight.SemiBold)},text={Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
  when(kind){
   "income_period"->{Text("Cada periodo conserva su propio ingreso real y rango de fechas.",color=Muted,fontSize=12.sp);Field("Ingreso real del periodo","amount",v,true);Field("Moneda base (igual para todos los periodos)","currency",v);Text("Frecuencia del ciclo",color=Muted,fontSize=12.sp);FrequencyChoice(v);DatePickerField("Fecha de inicio","start",v);DatePickerField("Fecha de fin","end",v)}
   "fixed"->{Field("Concepto (renta, luz…)","name",v);Field("Monto","amount",v,true);CurrencyRateFields(v,baseCurrency);Text("Frecuencia del cobro",color=Muted,fontSize=12.sp);FrequencyChoice(v);Field("Fecha (ej. día 15, viernes)","due",v)}
   "expense"->{Field("Comando (Gasto $120 tacos)","quick",v);Text("También puedes escribir: Tarjeta $500 comida. Se usará la primera tarjeta guardada.",color=Muted,fontSize=11.sp);CurrencyRateFields(v,baseCurrency);Text("Categoría",color=Muted,fontSize=12.sp);Column{listOf("Alimentos","Transporte","Servicios","Ocio","Salud","Otros").chunked(3).forEach{row->Row{row.forEach{c->FilterChip(selected=category==c,onClick={category=c;v["category"]=c},label={Text(c,fontSize=11.sp)})}}}}}
   "card"->{Field("Alias o banco (sin número)","name",v);Field("Límite de crédito","limit",v,true);Field("Día de corte (1-31)","cut",v,true);Field("Día límite de pago (1-31)","due",v,true);CurrencyRateFields(v,baseCurrency);Text("¿El pago saldrá del salario registrado?",fontSize=12.sp,color=Muted);Row{FilterChip(selected=salary,onClick={salary=true;v["salary"]="true"},label={Text("Sí, del salario")});FilterChip(selected=!salary,onClick={salary=false;v["salary"]="false"},label={Text("No, aparte")})};v["salary"]=salary.toString();if(salary)Field("Pago mensual estimado","payment",v,true)}
   "goal"->{Field("Nombre de la meta","name",v);Field("Monto objetivo","amount",v,true);CurrencyRateFields(v,baseCurrency);Text("La tasa queda guardada con la meta para calcular su apartado en moneda base.",color=Muted,fontSize=11.sp);DeadlinePicker(v)}
   "contribute"->{Text("${goal?.name.orEmpty()} · ${goal?.currency?:baseCurrency}",color=Muted,fontSize=12.sp);Field("Monto a aportar (${goal?.currency?:baseCurrency})","amount",v,true)}
   else->Field("Monto","amount",v,true)
  }
 }},confirmButton={Button(onClick={onSave(v.toMap()+mapOf("salary" to salary.toString(),"category" to category))},colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black),shape=RoundedCornerShape(12.dp)){Text("Guardar",fontWeight=FontWeight.Bold)}},dismissButton={TextButton(onClick=onClose){Text("Cancelar",color=Muted)}})
}

@Composable private fun CurrencyRateFields(v:MutableMap<String,String>,baseCurrency:String){
 val currency=v["currency"].orEmpty()
 OutlinedTextField(value=currency,onValueChange={input->
  val normalized=input.uppercase()
  if(normalized!=currency){v["currency"]=normalized;v["rate"]=if(normalized==baseCurrency.uppercase())"1" else ""}
 },label={Text("Código de divisa (p. ej. MXN, USD, EUR)",fontSize=12.sp)},modifier=Modifier.fillMaxWidth(),singleLine=true,colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Gold,focusedLabelColor=Gold))
 if(currency==baseCurrency.uppercase())Text("Moneda base: conversión 1:1",color=Muted,fontSize=11.sp)
 else {
  Field("Tasa manual: 1 $currency = ? $baseCurrency","rate",v,true)
  Text("Captura la tasa estimada que quieres usar; no se consulta una cotización en vivo.",color=Muted,fontSize=11.sp)
 }
}

@Composable private fun Field(label:String,key:String,v:MutableMap<String,String>,numeric:Boolean=false){OutlinedTextField(value=v[key].orEmpty(),onValueChange={v[key]=it},label={Text(label,fontSize=12.sp)},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Gold,focusedLabelColor=Gold))}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun DatePickerField(label:String,key:String,v:MutableMap<String,String>){
 var pickerOpen by remember(key){mutableStateOf(false)}
 OutlinedTextField(value=v[key].orEmpty(),onValueChange={},label={Text(label,fontSize=12.sp)},modifier=Modifier.fillMaxWidth().clickable{pickerOpen=true},readOnly=true,singleLine=true,trailingIcon={TextButton(onClick={pickerOpen=true}){Text("Elegir",color=Gold)}},colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Gold,focusedLabelColor=Gold))
 if(pickerOpen){
  val initialMillis=runCatching{LocalDate.parse(v[key]).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()}.getOrNull()
  val dateState=rememberDatePickerState(initialSelectedDateMillis=initialMillis)
  DatePickerDialog(onDismissRequest={pickerOpen=false},confirmButton={TextButton(onClick={dateState.selectedDateMillis?.let{selected->
   val chosen=selectedDateMillisToIsoDate(selected);v[key]=chosen
   if(key=="start"){val end=runCatching{LocalDate.parse(v["end"])}.getOrNull();if(end!=null&&end.isBefore(LocalDate.parse(chosen)))v["end"]=chosen}
  };pickerOpen=false}){Text("Seleccionar",color=Gold)}},dismissButton={TextButton(onClick={pickerOpen=false}){Text("Cancelar",color=Muted)}}){DatePicker(state=dateState,title={Text("Selecciona $label")})}
 }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun DeadlinePicker(v:MutableMap<String,String>){
 var pickerOpen by remember{mutableStateOf(false)}
 OutlinedTextField(value=v["deadline"].orEmpty(),onValueChange={},label={Text("Fecha límite (AAAA-MM-DD)",fontSize=12.sp)},modifier=Modifier.fillMaxWidth().clickable{pickerOpen=true},readOnly=true,singleLine=true,trailingIcon={TextButton(onClick={pickerOpen=true}){Text("Elegir",color=Gold)}},colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Gold,focusedLabelColor=Gold))
 if(pickerOpen){val initialMillis=runCatching{LocalDate.parse(v["deadline"]).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()}.getOrNull();val dateState=rememberDatePickerState(initialSelectedDateMillis=initialMillis);DatePickerDialog(onDismissRequest={pickerOpen=false},confirmButton={TextButton(onClick={dateState.selectedDateMillis?.let{v["deadline"]=selectedDateMillisToIsoDate(it)};pickerOpen=false}){Text("Seleccionar",color=Gold)}},dismissButton={TextButton(onClick={pickerOpen=false}){Text("Cancelar",color=Muted)}}){DatePicker(state=dateState,title={Text("Selecciona fecha límite")})}}
}

@Composable private fun FrequencyChoice(v:MutableMap<String,String>){Column{listOf(listOf("SEMANAL","CATORCENAL"),listOf("QUINCENAL","MENSUAL")).forEach{row->Row{row.forEach{f->FilterChip(selected=v["frequency"]==f,onClick={v["frequency"]=f},label={Text(when(f){"SEMANAL"->"Semanal";"CATORCENAL"->"Catorcenal";"QUINCENAL"->"Quincenal";else->"Mensual"})})}}}}}
