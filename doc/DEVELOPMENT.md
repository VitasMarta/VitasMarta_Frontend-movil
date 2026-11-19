# Desarrollo Android

## Entorno reproducible

JDK 17, Gradle 8.13 (wrapper oficial con SHA-256), AGP 8.13.2, Kotlin 2.1.20,
Compose BOM 2025.04.01. minSdk 26, compileSdk/targetSdk 36. Versiones fijadas en
`gradle/libs.versions.toml`; no se presentan como las últimas versiones disponibles.
El par AGP/Gradle/JDK está respaldado por
[la tabla oficial de compatibilidad](https://developer.android.com/build/releases/agp-8-13-0-release-notes).
Al preparar distribución se revisará targetSdk frente a las exigencias vigentes.

La instalación de esta sesión está en `.local-tools/android-sdk` y Gradle usa
`.local-tools/gradle-home`; ambas están ignoradas. `local.properties` contiene la ruta
local del SDK y no se versiona. Android Studio puede generar ese archivo normalmente.
No se necesita instalar SDK/Gradle dentro de otros repositorios.

## Gateway configurable

Opciones, en orden de prioridad:

1. Propiedad Gradle `-Papi.gateway.url=...`.
2. Variable `VITASMARTA_API_BASE_URL`.
3. `api.gateway.url` en `local.properties` (ver local.properties.example).

El valor debe ser un origen HTTP o HTTPS válido; HTTPS conserva la validación de certificado;
no debe contener credenciales, query, fragmento ni prefijos de API. Todas las rutas
Retrofit se resuelven bajo `/api/v1/` directamente en M1. No usar `/bff` de Next.
No poner tokens ni contraseñas en ninguna de estas opciones.

Debug puede compilar sin URL: no construye Retrofit ni genera un origen ficticio.
Release falla si falta la URL; HTTP se acepta también en release por decisión del propietario.
El build genera gateway_network_security.xml: cleartext denegado por defecto y permitido
únicamente para el host del origen si su esquema es HTTP, sin subdominios. El manifest
referencia ese recurso y conserva usesCleartextTraffic=false como valor de respaldo.
El interceptor exige el mismo esquema, host, puerto y prefijo /api/v1/.
No hay excepciones de certificado,
trust-all, logging de cuerpos ni seguimiento de redirecciones.
El cliente de Gateway debe usarse exclusivamente para negocio; no reutilizarlo para CDN.

## Verificación

Con JDK 17 y SDK configurado:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:ktlintCheck :app:assembleDebugAndroidTest
```

Corrección de formato: `./gradlew :app:ktlintFormat`.
Con dispositivo/emulador: `./gradlew :app:connectedDebugAndroidTest`.
La CI de GitHub realiza build, unitarias, lint y formato; se entrega su configuración,
pero no se declara que un workflow remoto haya corrido sin evidencia.

El script `scripts/verify-android.sh` configura rutas absolutas de caché/usuario Android
dentro de `.local-tools/`, evitando rutas relativas incorrectas de debug.keystore.
En esta máquina se usaron `JAVA_HOME=/usr/lib/jvm/java-17-openjdk`,
`GRADLE_USER_HOME=.local-tools/gradle-home` y
`ANDROID_USER_HOME=.local-tools/android-user` al ejecutar Gradle. Son ajustes locales,
no rutas que otro desarrollador deba copiar obligatoriamente.

## Estado del servidor recibido

Se consultaron únicamente recursos públicos, sin credenciales. La URL HTTP facilitada
por el propietario devolvió 200 con OpenAPI 3.0.1, `paths: {}`, `components: {}` y
`x-servicios-no-disponibles` para los nueve servicios. La conexión HTTPS agotó
5 segundos sin establecerse. La dirección queda en la conversación, no embebida en código.
No se hicieron login, reservas ni mutaciones sobre ese despliegue.
GET categorías devolvió 200 con seis entradas y nombres es/en; GET búsqueda con limite=1
devolvió 200 con items vacíos, total y cursor_siguiente. El OpenAPI vacío no implica que
los servicios estén caídos. El propietario autorizó configurar HTTP el 2026-11-18;
las referencias previas a esperar HTTPS quedan sustituidas por esa decisión.

## Fuentes y licencias

Poppins 400/500/600/700 se descarga de `google/fonts`, directorio `ofl/poppins`;
la licencia SIL OFL está en `doc/licenses/Poppins-OFL.txt` y empaquetada en assets.
La hoja del ícono adapta el path existente en `src/app/icon.svg` del frontend web
fijado en PHASE_0.md. No se incluyen sus fotos de demostración ni mocks.

## Orden de trabajo autorizado

El propietario autorizó avanzar mientras consigue la URL del Gateway, y después
facilitó una dirección HTTP. Se permite completar/verificar la base y cortes con
contrato conocido sin esperar un despliegue; Fase 0 conserva sus bloqueos de
confirmación y contratos. No se marcan pruebas reales como aprobadas ni se
avanza a funcionalidades de contrato ambiguo por suposición. Alcance turista,
namespace y minSdk se usan provisionalmente conforme a las especificaciones,
sin excluir definitivamente otros roles.


## Sesión implementada

Access y refresh permanecen únicamente en memoria; se pierden al terminar el proceso.
No hay “recordarme”, DataStore de credenciales ni persistencia de password. Una futura
sesión persistente deberá implementar Keystore y pruebas de cifrado/restauración antes
de habilitarse. Logout invalida inmediatamente la generación local, incluso si falla
la revocación remota. Refresh usa Mutex y rotación atómica; los requests privados
reintentan una vez, conservan correlación y descartan resultados de sesiones anteriores.

`ApiExecutor` distingue fallos de transporte/esquema y propaga cancelación. `Retry-After`
admite segundos y fecha HTTP, y el reintento de UI respeta el tiempo indicado. No hay
reintentos automáticos de mutaciones ni logging de bodies/tokens.

Las advertencias de actualizaciones de dependencias de lint se mantienen informativas
por política de versiones fijadas. El resto de warnings siguen siendo errores. No se
creó baseline de lint ni se silenciaron seguridad, accesibilidad o compatibilidad.

## Apariencia y pruebas en dispositivo

El botón de paleta de la barra superior abre Sistema/Claro/Oscuro sin requerir sesión.
DataStore guarda solo esta preferencia, sin credenciales. Sistema elimina la selección
explícita y sigue isSystemInDarkTheme; el splash espera la lectura antes de mostrar Compose.
AndroidX SplashScreen 1.0.1 gestiona el arranque y las barras siguen el tema efectivo.

AndroidX Test usa ext-junit 1.3.0, Espresso 3.7.0 y runner 1.7.0. NativeUiTest pone
la Activity de esta app en primer plano antes de la regla Compose por restricciones de
arranque OEM en el teléfono físico Android 15. No requiere root ni cambios de seguridad.
Las instrumentadas pueden desinstalar el APK al terminar; reinstalar el debug generado
para pruebas manuales. Las capturas se escriben en cache/screenshots de la app de test.

## Pruebas con Gateway configurado

Los tests JVM son deterministas y no consultan el despliegue. Las instrumentadas
comprueban la política Android de cleartext en todos los builds. El caso de reintento
sin configuración solo aplica cuando API_BASE_URL está vacía; se omite con un servidor.
La prueba pública real es opt-in: `:app:connectedDebugAndroidTest
-Pandroid.testInstrumentationRunnerArguments.gatewaySmoke=true`. Requiere origen local
configurado; espera el catálogo real habilitado y no envía credenciales ni mutaciones.
Sin esa opción, la prueba real se omite para que CI no dependa del despliegue.

HTTP no cifra el tráfico; la autorización del propietario cambia la política del proyecto,
no las propiedades de seguridad del protocolo. El recurso generado queda bajo build/,
y el host recibido no se guarda en fuentes o documentación versionada.
Referencia: [Network Security Configuration de Android](https://developer.android.com/privacy-and-security/security-config).

## Registro nativo

Perfil y destinos privados sin sesión muestran Crear cuenta antes de los campos.
RegistrationApi llama POST /api/v1/identidad/cuentas sin Bearer; RegistrationRepository
expone un resultado sin tokens. DTO M7 confirmado en commit 95f1d32c, ver API_INVENTORY.md.
La pantalla no pregunta nombre ni confirmación de contraseña porque el endpoint no
los exige; usa NewUsername/EmailAddress y NewPassword, sin bloquear pegar contraseñas.
Los tres consentimientos parten de false; tratamiento obligatorio y opcionales separados.
Una respuesta 201 con mensaje confirma creación; no confirma verificación del correo
ni instala sesión. La app pide abrir el enlace enviado por M7/M10 y luego volver a acceso.
No se añaden App Links sin dominio verificado ni se inventa una URL de activación.

Las pruebas automáticas no crean usuarios reales ni envían correos reales. La instrumentada
solo navega, valida campos vacíos y consentimientos, y captura el formulario sin PII.

## Integración del flujo turista (2026-11-18)

Rutas tipadas secundarias para búsqueda, ficha, ofertas y reserva. Las pestañas
privadas usan acceso contextual y estado por generación de sesión; logout elimina
las pilas privadas y descarta cargas tardías. La memoria de tokens no sobrevive
a muerte de proceso. Idioma: LocaleManager API33+, Context/prefs locales en API26–32;
recursos es-CO/en-US siempre empaquetados. Apariencia usa DataStore.

PdfDocumentHost y PdfTransferViewModel retienen bytes únicamente en memoria durante
rotación y verifican generación antes de escribir al URI elegido. Tras muerte de
proceso se solicita repetir la descarga; no se escribe un archivo vacío silencioso.
Un intento de reserva ambiguo conserva su clave en memoria; muerte del proceso
requiere consultar historial antes de iniciar otra operación. No hay cola persistente
ni reenvío automático de mutaciones.

Release local: ./scripts/verify-android.sh :app:bundleRelease :app:assembleRelease.
Se verificó R8, sin clave de distribución ni publicación. No usar clave debug
como firma de producción. No se requieren cuentas reales para los tests JVM;
la suite instrumentada gatewaySmoke=true solo lee catálogo público. Los fixtures
de UI de reservas/recomendaciones pertenecen exclusivamente a androidTest.
