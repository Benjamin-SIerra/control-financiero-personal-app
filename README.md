# Control Financiero Personal (aplicación nueva)

Proyecto Android nativo **independiente**, creado desde cero. No reemplaza ni modifica otros repositorios o apps anteriores.

- Kotlin, Jetpack Compose y Material 3.
- Room/SQLite local. No solicita permiso de Internet.
- Interfaz en español, carbón oscuro y acento oro champagne.
- Sin número de tarjeta, CVV, NIP, cuentas bancarias ni datos de autenticación.

## Funciones

- Periodos de ingreso independientes con monto real, rango de fechas (inicio/fin) y frecuencia semanal, catorcenal, quincenal o mensual; conserva el histórico y permite editar o eliminar con confirmación.
- Gastos fijos/recurrentes con frecuencia y fecha de cobro; apartado proporcional al ciclo de pago.
- Resumen de ingreso, apartado, ahorro sugerido, gastos del periodo y saldo libre.
- Gastos rápidos con comandos de texto: `Gasto $120 tacos` y `Tarjeta $500 comida`.
- Tarjetas representadas por alias, límite, saldo utilizado, días de corte/pago y abonos. Se pregunta si el pago mensual estimado se separa del sueldo o de otro presupuesto.
- Metas con moneda, fecha objetivo y sugerencia de aporte por periodo.
- Tipo de cambio manual para convertir montos a la moneda base (no se consulta la red).
- Alertas preventivas por saldo libre bajo/agotado y utilización de crédito alta.

### Ingresos / Periodos

Cada ingreso se guarda como un registro con fechas inclusivas de inicio y fin, frecuencia, moneda base y monto real. Los rangos que se traslapan se rechazan y todos los periodos usan la misma moneda base. Al rebasar la fecha de fin, el registro permanece como histórico y la app abre un periodo nuevo sin copiar el monto anterior; queda marcado como pendiente hasta que se capture el ingreso real. El periodo vigente alimenta el saldo libre y determina qué gastos pertenecen al ciclo. Las fechas se eligen con el DatePicker de Material 3. La migración Room 1→2 conserva el ingreso de perfil existente como periodo vigente.

## Cálculo

Los montos se guardan como unidades menores enteras (centavos). Para convertir gastos a moneda base se guarda el tipo de cambio que ingresó el usuario. El apartado de cada gasto se calcula como `monto × cobros/año ÷ pagos de sueldo/año` (semanal 52, catorcenal 26, quincenal 24 y mensual 12). Las metas distribuyen el restante entre los pagos estimados hasta la fecha límite. El saldo libre es ingreso del periodo menos apartados, metas y gastos registrados dentro del periodo de sueldo.

La captura rápida es texto; la primera tarjeta guardada se asigna cuando el comando comienza con `Tarjeta`. El acceso a voz/micrófono no se solicita para minimizar permisos y no transmitir audio.

## Compilar

Usar JDK 17, Android SDK 35 y Gradle 8.10.2; ejecutar `testDebugUnitTest assembleDebug`. APK debug en `app/build/outputs/apk/debug/app-debug.apk`.
