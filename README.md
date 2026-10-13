# Control Financiero Personal (aplicación nueva)

Proyecto Android nativo **independiente**, creado desde cero. No reemplaza ni modifica otros repositorios o apps anteriores.

- Kotlin, Jetpack Compose y Material 3.
- Room/SQLite local. No solicita permiso de Internet.
- Interfaz en español, carbón oscuro y acento oro champagne.
- Sin número de tarjeta, CVV, NIP, cuentas bancarias ni datos de autenticación.

## Funciones

- Ingreso por sueldo y frecuencia semanal, catorcenal, quincenal o mensual.
- Gastos fijos/recurrentes con frecuencia y fecha de cobro; apartado proporcional al ciclo de pago.
- Resumen de ingreso, apartado, ahorro sugerido, gastos del periodo y saldo libre.
- Gastos rápidos con comandos de texto: `Gasto $120 tacos` y `Tarjeta $500 comida`.
- Tarjetas representadas por alias, límite, saldo utilizado, días de corte/pago y abonos. Se pregunta si el pago mensual estimado se separa del sueldo o de otro presupuesto.
- Metas con moneda, fecha objetivo y sugerencia de aporte por periodo.
- Tipo de cambio manual para convertir montos a la moneda base (no se consulta la red).
- Alertas preventivas por saldo libre bajo/agotado y utilización de crédito alta.

## Cálculo

Los montos se guardan como unidades menores enteras (centavos). Para convertir gastos a moneda base se guarda el tipo de cambio que ingresó el usuario. El apartado de cada gasto se calcula como `monto × cobros/año ÷ pagos de sueldo/año` (semanal 52, catorcenal 26, quincenal 24 y mensual 12). Las metas distribuyen el restante entre los pagos estimados hasta la fecha límite. El saldo libre es ingreso del periodo menos apartados, metas y gastos registrados dentro del periodo de sueldo.

La captura rápida es texto; la primera tarjeta guardada se asigna cuando el comando comienza con `Tarjeta`. El acceso a voz/micrófono no se solicita para minimizar permisos y no transmitir audio.

## Compilar

Usar JDK 17, Android SDK 35 y Gradle 8.10.2; ejecutar `testDebugUnitTest assembleDebug`. APK debug en `app/build/outputs/apk/debug/app-debug.apk`.
