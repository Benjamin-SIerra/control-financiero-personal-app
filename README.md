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
- Registro de gastos estructurado: categoría obligatoria de una lista, monto numérico mayor que cero y elección explícita de efectivo o tarjeta; no acepta texto libre como registro.
- En ciclos nuevos, detecta el remanente positivo del periodo anterior y pregunta si se suma al saldo disponible. La respuesta se guarda por ciclo.
- Ajuste manual del saldo libre del periodo activo (admite saldo negativo); queda limitado al ciclo en curso y no reescribe saldos ni registros históricos o futuros.
- Tarjetas representadas por alias, límite, saldo utilizado, días de corte/pago y abonos. Se pregunta si el pago mensual estimado se separa del sueldo o de otro presupuesto.
- Metas de ahorro con crear, editar, aportar, cancelar, completar y eliminar con confirmación. Las metas activas apartan una estimación por pago; al cancelar/completar/eliminar o alcanzar el objetivo se libera ese apartado y se recalcula el saldo libre.
- Gastos y metas multidivisa: la persona captura la tasa de cambio estimada 1 unidad extranjera = X unidades de moneda base. La tasa queda guardada con el movimiento/meta y se usa en el saldo; no se descarga cotización en vivo.
- Alertas preventivas por saldo libre bajo/agotado y utilización de crédito alta.

### Ingresos / Periodos

Cada ingreso se guarda como un registro con fechas inclusivas de inicio y fin, frecuencia, moneda base y monto real. Los rangos que se traslapan se rechazan y todos los periodos usan la misma moneda base. Al rebasar la fecha de fin, el registro permanece como histórico y la app abre un periodo nuevo sin copiar el monto anterior; queda marcado como pendiente hasta que se capture el ingreso real. El periodo vigente alimenta el saldo libre y determina qué gastos pertenecen al ciclo. Las fechas se eligen con el DatePicker de Material 3. La migración Room 1→2 conserva el ingreso de perfil existente como periodo vigente.

## Cálculo

Los montos se guardan como unidades menores enteras (centavos). Para convertir gastos, tarjetas, recurrentes y metas a moneda base se guarda la tasa manual indicada por el usuario (1 unidad de divisa = X unidades base); las tasas no se descargan ni actualizan automáticamente. El apartado de cada gasto se calcula como `monto × cobros/año ÷ pagos de sueldo/año` (semanal 52, catorcenal 26, quincenal 24 y mensual 12). Solo las metas activas distribuyen su restante convertido entre los pagos estimados hasta la fecha límite. El saldo libre es ingreso del periodo menos apartados activos y gastos convertidos registrados dentro del periodo de sueldo. Room migra v1→v2 preservando el periodo de ingreso, v2→v3 agregando estado a las metas y v3→v4 agregando la tabla de remanente/ajuste por periodo sin borrar datos.

La captura de gastos es estructurada: categoría de lista cerrada, monto numérico positivo y elección efectivo/tarjeta. El acceso a voz/micrófono no se solicita para minimizar permisos y no transmitir audio.

El arrastre usa una tabla por periodo (`period_balance_adjustments`) con el remanente decidido y el ajuste manual. El ajuste manual se calcula como la diferencia necesaria para alcanzar el saldo real capturado (`objetivo − saldo calculado − arrastre`) y se guarda solo para el periodo activo; los periodos históricos y futuros no se recalculan ni sobrescriben. El arrastre solo se ofrece cuando el periodo anterior cerró con saldo positivo.

## Compilar

Usar JDK 17, Android SDK 35 y Gradle 8.10.2; ejecutar `testDebugUnitTest assembleDebug` y `python scripts/verify_goal_migration.py` para validar la migración 2→3 contra SQLite. APK debug en `app/build/outputs/apk/debug/app-debug.apk`.


## Arquitectura

El proyecto prioriza una separación sencilla y comprobable de responsabilidades:

```text
app/src/main/java/com/benjamin/controlfinanciero/
├── data/       # Room/SQLite, entidades, migraciones y repositorio transaccional
├── domain/     # Reglas de cálculo, prorrateos y validaciones puras
├── ui/          # Composables, diálogos y ViewModel
└── MainActivity.kt
```

La información financiera se conserva en el dispositivo. La aplicación no depende de servicios remotos, no integra analítica y no solicita permiso de Internet.

## Inicio rápido

### Requisitos

- Android Studio Ladybug o posterior.
- JDK 17.
- Android SDK Platform 35.
- Un emulador o dispositivo Android con API 24 o superior.

### Clonar y ejecutar

```bash
git clone https://github.com/Benjamin-SIerra/control-financiero-personal-app.git
cd control-financiero-personal-app
```

1. Abre la carpeta clonada desde **Android Studio**.
2. Espera a que Gradle sincronice las dependencias.
3. Selecciona un emulador o dispositivo Android.
4. Ejecuta la configuración **app** con el botón **Run**.

Para validar desde la terminal:

```bash
./gradlew testDebugUnitTest assembleDebug
python scripts/verify_goal_migration.py
python scripts/verify_period_balance_migration.py
```

> En Windows se puede usar `gradlew.bat` desde PowerShell o Android Studio. El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`.

## Privacidad y seguridad

- No incluyas datos reales de cuentas, tarjetas, identificaciones ni movimientos personales en issues, capturas o commits.
- `local.properties`, `.env*`, archivos de firma, claves, certificados y `google-services.json` están ignorados por Git.
- Si en el futuro se integra una API (por ejemplo, Gemini), utiliza `local.properties` o `secrets.properties` solo en el equipo local y proporciona una plantilla sin valores reales. Nunca publiques una clave en el código ni en el historial Git.

## Calidad para portafolio

Para mantener el repositorio público listo para revisión profesional:

- Mantén capturas con datos ficticios y una breve demostración en video/GIF.
- Añade pruebas unitarias a cada regla nueva de prorrateo, moneda o migración.
- Usa commits pequeños con mensajes descriptivos y una rama/PR para cambios relevantes.
- Ejecuta la suite de pruebas y una compilación limpia antes de etiquetar una versión.
- La licencia MIT incluida permite reutilización conforme a sus términos.

## Estado

Proyecto de portafolio en evolución. No es asesoría financiera; los cálculos son herramientas de organización personal y dependen de los datos capturados por la persona usuaria.
