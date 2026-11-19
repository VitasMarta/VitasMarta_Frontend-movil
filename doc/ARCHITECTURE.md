# Arquitectura técnica

## Decisiones propuestas
Kotlin, Compose Material 3, Navigation Compose, Lifecycle/ViewModel, Coroutines/Flow, Hilt, Retrofit + OkHttp, Kotlinx Serialization, Coil, DataStore para preferencias no sensibles. Para credenciales persistentes usar cifrado respaldado por Android Keystore; no DataStore en texto plano. Gradle Kotlin DSL y catálogo libs.versions.toml. Elegir versiones estables compatibles con el SDK/AGP instalado; minSdk 26 sugerido (validar audiencia), targetSdk según requisitos vigentes de distribución. JUnit, coroutines-test, MockWebServer, Compose UI Test; ktlint o equivalente. Room solo si existe necesidad offline confirmada.

## Organización inicial
Un módulo Gradle :app con paquetes por feature para evitar sobreingeniería; extraer módulos Gradle solo cuando haya necesidad medible.

```text
app/src/main/java/co/vitasmarta/mobile/   # namespace provisional
  app/                # Application, MainActivity, configuración
  navigation/         # rutas tipadas, grafos, guards
  core/
    network/          # Retrofit, interceptores, errores
    auth/             # sesión, renovación y almacenamiento seguro
    designsystem/     # theme, tokens y componentes
    common/           # utilidades realmente compartidas
    di/               # bindings globales
  feature/
    auth/
    catalog/
    booking/
    recommendations/
    profile/
    provider/         # fase posterior
    # cada feature según necesidad:
    # data/{remote,dto,mapper,repository}
    # domain/{model,repository,usecase}
    # presentation/{screen,component,viewmodel,state}
app/src/main/res/     # strings, recursos y variantes de idioma
app/src/test/        # unitarias
app/src/androidTest/ # instrumentadas
```

## Dependencias y flujo
UI → ViewModel → repositorio (o caso de uso si hay lógica reusable) → fuente remota → API Gateway. Los modelos de dominio no importan Android ni Retrofit; data implementa interfaces de dominio; composition root Hilt realiza el ensamblaje. Una feature no importa internals de otra; navegación y contratos explícitos para integración. No crear casos de uso que solo deleguen sin valor. Composables de pantalla reciben estado inmutable y callbacks; Route conecta ViewModel y navegación. StateFlow y collectAsStateWithLifecycle; flujo unidireccional. SavedStateHandle para argumentos; no transportar objetos grandes por navegación. Cancelar búsquedas anteriores, debounce de texto, evitar doble envío y actualizaciones tras logout. No GlobalScope ni runBlocking en UI.

## Calidad y seguridad
Serializar DTO según contrato y mapear a dominio, tolerar campos opcionales sin ocultar errores de esquema. No usar!! salvo invariantes justificadas. Manejar CancellationException sin convertirla en fallo. Fechas ISO-8601 y zona horaria explícita; dinero con precisión decimal y moneda del contrato. Transporte HTTP o HTTPS al origen explícitamente configurado del Gateway (decisión del propietario, 2026-11-18). HTTP no cifra credenciales ni tokens. Sin trust-all, sin bypass de certificados ni PII en logs o crash reports. Excluir credenciales de backups. Permisos mínimos y solicitud contextual. Separar debug/release, base URL configurable, validar host y evitar enviar Authorization a CDN. Tests deterministas y sin red real en CI. Usar cuentas de prueba autorizadas para smoke tests.

## Entrega
CI ejecuta compilación, unitarias, lint y formato. Release firmado con claves fuera del repo, AAB y APK de prueba, versionCode incremental. Revisar manifest, permisos, accesibilidad, traducciones, privacidad, eliminación de cuenta y requisitos Play vigentes antes de publicar.
