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
import com.benjamin.controlfinanciero.domain.selectedDateMillisToIsoDate

@Composable fun FinanceDialog(kind:String,baseCurrency:String,onClose:()->Unit,onSave:(Map<String,String>)->Unit){
 val v=remember(kind){mutableStateMapOf<String,String>()};var salary by remember(kind){mutableStateOf(true)};var category by remember(kind){mutableStateOf("Otros")}
 LaunchedEffect(kind,baseCurrency){v["currency"]=baseCurrency;v["rate"]="1";v["frequency"]="QUINCENAL";v["deadline"]=LocalDate.now().plusMonths(3).toString();v["category"]="Otros"}
 val title=when(kind){"income"->"Ingreso principal";"fixed"->"Gasto recurrente";"expense"->"Registrar gasto";"card"->"Nueva tarjeta";"pay"->"Registrar abono";"goal"->"Nueva meta";else->"Aportación a meta"}
 AlertDialog(onDismissRequest=onClose,title={Text(title,fontWeight=FontWeight.SemiBold)},text={Column(Modifier.fillMaxWidth().heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
  when(kind){
   "income"->{Field("Ingreso por pago","amount",v,true);Field("Moneda principal (MXN, USD…)","currency",v);Text("¿Cada cuándo recibes el ingreso?",color=Muted,fontSize=12.sp);FrequencyChoice(v)}
   "fixed"->{Field("Concepto (renta, luz…)","name",v);Field("Monto","amount",v,true);Field("Moneda","currency",v);Field("1 unidad equivale a cuánto en moneda base","rate",v,true);Text("Frecuencia del cobro",color=Muted,fontSize=12.sp);FrequencyChoice(v);Field("Fecha (ej. día 15, viernes)","due",v)}
   "expense"->{Field("Comando (Gasto $120 tacos)","quick",v);Text("También puedes escribir: Tarjeta $500 comida. Se usará la primera tarjeta guardada.",color=Muted,fontSize=11.sp);Field("Moneda del gasto","currency",v);Field("Tipo de cambio a tu moneda base","rate",v,true);Text("Categoría",color=Muted,fontSize=12.sp);Column{listOf("Alimentos","Transporte","Servicios","Ocio","Salud","Otros").chunked(3).forEach{row->Row{row.forEach{c->FilterChip(selected=category==c,onClick={category=c;v["category"]=c},label={Text(c,fontSize=11.sp)})}}}}}
   "card"->{Field("Alias o banco (sin número)","name",v);Field("Límite de crédito","limit",v,true);Field("Día de corte (1-31)","cut",v,true);Field("Día límite de pago (1-31)","due",v,true);Field("Moneda de la tarjeta","currency",v);Field("Tipo de cambio a moneda base","rate",v,true);Text("¿El pago saldrá del salario registrado?",fontSize=12.sp,color=Muted);Row{FilterChip(selected=salary,onClick={salary=true;v["salary"]="true"},label={Text("Sí, del salario")});FilterChip(selected=!salary,onClick={salary=false;v["salary"]="false"},label={Text("No, aparte")})};v["salary"]=salary.toString();if(salary)Field("Pago mensual estimado","payment",v,true)}
   "goal"->{Field("Nombre de la meta","name",v);Field("Monto objetivo","amount",v,true);Field("Moneda","currency",v);Field("Tipo de cambio a moneda base","rate",v,true);DeadlinePicker(v)}
   else->Field("Monto","amount",v,true)
  }
 }},confirmButton={Button(onClick={onSave(v.toMap()+mapOf("salary" to salary.toString(),"category" to category))},colors=ButtonDefaults.buttonColors(containerColor=Gold,contentColor=Color.Black),shape=RoundedCornerShape(12.dp)){Text("Guardar",fontWeight=FontWeight.Bold)}},dismissButton={TextButton(onClick=onClose){Text("Cancelar",color=Muted)}})
}

@Composable private fun Field(label:String,key:String,v:MutableMap<String,String>,numeric:Boolean=false){OutlinedTextField(value=v[key].orEmpty(),onValueChange={v[key]=it},label={Text(label,fontSize=12.sp)},modifier=Modifier.fillMaxWidth(),singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=if(numeric)KeyboardType.Decimal else KeyboardType.Text),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Gold,focusedLabelColor=Gold))}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun DeadlinePicker(v:MutableMap<String,String>){
 var pickerOpen by remember{mutableStateOf(false)}
 OutlinedTextField(
  value=v["deadline"].orEmpty(),
  onValueChange={},
  label={Text("Fecha límite (AAAA-MM-DD)",fontSize=12.sp)},
  modifier=Modifier.fillMaxWidth().clickable{pickerOpen=true},
  readOnly=true,
  singleLine=true,
  trailingIcon={TextButton(onClick={pickerOpen=true}){Text("Elegir",color=Gold)}},
  colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Gold,focusedLabelColor=Gold)
 )
 if(pickerOpen){
  val initialMillis=runCatching{LocalDate.parse(v["deadline"]).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()}.getOrNull()
  val dateState=rememberDatePickerState(initialSelectedDateMillis=initialMillis)
  DatePickerDialog(
   onDismissRequest={pickerOpen=false},
   confirmButton={TextButton(onClick={dateState.selectedDateMillis?.let{v["deadline"]=selectedDateMillisToIsoDate(it)};pickerOpen=false}){Text("Seleccionar",color=Gold)}},
   dismissButton={TextButton(onClick={pickerOpen=false}){Text("Cancelar",color=Muted)}}
  ){
   DatePicker(state=dateState,title={Text("Selecciona fecha límite")})
  }
 }
}

@Composable private fun FrequencyChoice(v:MutableMap<String,String>){Column{listOf(listOf("SEMANAL","CATORCENAL"),listOf("QUINCENAL","MENSUAL")).forEach{row->Row{row.forEach{f->FilterChip(selected=v["frequency"]==f,onClick={v["frequency"]=f},label={Text(when(f){"SEMANAL"->"Semanal";"CATORCENAL"->"Catorcenal";"QUINCENAL"->"Quincenal";else->"Mensual"})})}}}}}
