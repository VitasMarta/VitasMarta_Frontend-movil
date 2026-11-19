# Evidencia de implementación — actualizada 2026-11-18

Rama `feature/phase-0-discovery`, base `d49342fb9bfcfb0beb831966e513e787d1b44dbf`.
Solo se modifica el repositorio móvil. El propietario autorizó fetch/pull/commit/push y PR, excluyendo merge; se prepara entrega en esta rama.

## Corte vigente: catálogo, reservas, perfil y recomendaciones

Este corte sustituye las descripciones de funciones pendientes y hashes históricos
que siguen más abajo; se conservan para trazabilidad.

Las categorías abren búsqueda real con filtros/orden/cursor; la ficha consulta M2
y las ofertas M3 aportan servicio_ref/franja_ref. Reservas soporta política vigente,
aceptación, creación idempotente, paginación por grupo, detalle, cancelación,
modificación por reemplazo y PDF. Los reintentos ambiguos conservan clave/correlación;
no se reenvían mutaciones automáticamente. Perfil consume M7: nombre, idioma,
consentimientos independientes, reemplazo explícito de intereses, ARCO y PDF.
M6 usa recomendaciones[] y conserva 206 como éxito degradado. No se llama al
GET intereses ni /alternativas/{id}: no existen handlers en las fuentes auditadas.

Código de dominio separado de DTO/Retrofit y Compose, MVVM/StateFlow, Hilt y
organización feature-first. Generación de sesión protege navegación y cargas tardías
incluso al volver a la misma cuenta. Importes BigDecimal con límites previos a
expansión de exponentes; ausencia no se convierte en cero. PDF privado validado
por MIME, cabecera y máximo 10MiB; selector Android, memoria durante rotación
y descarte tras cambio de sesión. No hay mocks de producción.

Verificación secuencial por fases: verify-android.sh BUILD SUCCESSFUL en cada corte,
104 pruebas JVM, cero fallos/errores/omitidas; lint cero errores/advertencias y
ktlint aprobado. :app:bundleRelease :app:assembleRelease BUILD SUCCESSFUL con R8.
APK release sin clave de distribución; no se firma para publicar ni se publica.
Suite física vigente de once casos: diez aprobados y uno omitido; cero fallos.
Incluye listado público real → ficha real → acceso contextual a reserva.

### Artefactos actuales

- `app/build/outputs/apk/debug/app-debug.apk`: 20521893 bytes; SHA-256 `1ec465883e5fb0d49c142f0033f17dc61af3aaf0445a00b7c504e2b62e5f00df`.
- `app/build/outputs/apk/release/app-release-unsigned.apk`: 2659651 bytes; SHA-256 `f745f68421c6b3f7a5a9802328a6661b9db296b6ed04d5e78c6b6a302826fbbe`.
- `app/build/outputs/bundle/release/app-release.aab`: 5075332 bytes; SHA-256 `adbea4a286fecf16026cdf385bd2fc9176f2194082f7331783127564c68eacfd`.

Planes, contratos y decisiones visuales: [integración](../design/NATIVE_INTEGRATION.md),
[catálogo](../design/CATALOG_IMPLEMENTATION.md), [reservas](../design/BOOKING_IMPLEMENTATION.md),
[perfil y recomendaciones](../design/PROFILE_RECOMMENDATIONS_IMPLEMENTATION.md).

Revisión posterior: el servidor ahora devuelve tres contenidos públicos, fichas 200
y franjas informativas vacías. Ver [revisión vigente](REPOSITORY_RECHECK_2026-11-18.md).
Limitaciones: no se ejecutaron
creaciones/cancelaciones/modificaciones de reservas ni cambios ARCO/consentimientos
reales. Tests de transporte usan respuestas sintéticas solo en src/test; las dos
nuevas pruebas de pantallas reserva/recomendación usan fixtures solo en androidTest.
No equivalen a reservas reales ni recomendaciones personalizadas reales.
Identidad avanzada, sesión persistente, TalkBack/multidispositivo y firma de
distribución siguen pendientes. CI ejecutada en PR; corrección de preparación SDK en verificación remota.

## Registro histórico: alcance antes de esta integración

Fase 1: proyecto Android, wrapper oficial con checksum, catálogo, CI configurada,
Hilt, tema propio, Poppins local/licencia, recursos bilingües, navegación tipada,
Retrofit/OkHttp HTTP/HTTPS y errores. Compilado antes de avanzar a Fase 2.

Fase 2 parcial: categorías públicas; login/logout y refresh single-flight; tokens
en memoria; generación de sesión para evitar restaurar credenciales/datos tardíos;
registro de turista con confirmación por correo y recuperación inicial. Organización feature-first, domain sin Android/Retrofit,
data implementa repositorios y presentación usa ViewModel/StateFlow.

Las pantallas de reservas/recomendaciones aún muestran acceso y estado no disponible;
no consultan ni presentan datos simulados. Búsqueda, ficha, disponibilidad, reservas,
perfil y recomendaciones completos continúan pendientes de contratos y fases posteriores.
No hay sesión persistente, MFA completo, OAuth ni restablecimiento de password.

## Comandos y resultados observados

- `./scripts/verify-android.sh` con JDK 17: **BUILD SUCCESSFUL**, tareas de compilación y verificación completadas.
  Ejecuta assembleDebug, testDebugUnitTest, lintDebug, ktlintCheck y assembleDebugAndroidTest.
- Formato ejecutado previamente y por separado: `:app:ktlintFormat`, exitoso.
- JUnit: **44 pruebas, 0 fallos, 0 errores, 0 omitidas**.
- Lint: **0 errores, 0 warnings**. Avisos de nuevas versiones quedan informativos;
  no hay baseline y se siguen rechazando defectos de seguridad/compatibilidad.
- `:app:preReleaseBuild` sin origen: falla esperada por URL explícita obligatoria (comprobación histórica).
- La política anterior rechazaba HTTP; desde 2026-11-18 se acepta por decisión del propietario.
- `:app:preReleaseBuild` con el origen HTTP configurado: BUILD SUCCESSFUL.
- APK inspeccionado con aapt2/ZIP: minSdk 26, targetSdk 36, permiso INTERNET,
  usesCleartextTraffic=false como respaldo y networkSecurityConfig por host, allowBackup=false, fullBackupContent=false y reglas
  de extracción para Android 12+. Licencia Poppins presente; sin assets mock ni .env.

Resultado generable: `app/build/outputs/apk/debug/app-debug.apk`, 20 207 022 bytes.
SHA-256: `80ab0696215241f0355e81dbd652c8a4984510e174e27fe42fb99c213884c431`.
El APK está firmado con clave debug local; no es una release firmada para distribución.

## Cobertura local

| Suite | Casos |
|---|---|
| GatewayConfigurationTest | URL ausente sin inventar origen; aceptación HTTP/HTTPS; rechazo de otros esquemas/credenciales/prefijos/query/fragmento |
| GatewayBoundaryInterceptorTest | HTTP local y HTTPS con CA de test; locale/correlación; rechazo de otro esquema/host/puerto y BFF |
| ApiExecutorTest | 206 válido, body requerido, errores 400/401/403/409/423/429/503, envoltura malformada, campos seguros, timeout/offline y cancelación |
| SessionStoreTest | Claims inválidos/expirados, logout contra respuesta tardía, toString redactado |
| RemoteAuthRepositoryTest | Payload login, recuperación 202, login inválido/MFA sin sesión, diez refresh concurrentes, logout durante refresh, 401 frente a 503 y revocación fallida |
| AuthenticatedApiExecutorTest | Reintento único con correlación idéntica; descartar respuesta privada tras logout |
| RemoteCatalogRepositoryTest | Categorías bilingües anónimas, configuración ausente y esquema inválido |
| AuthViewModelTest | Validación, doble toque y limpieza del password |
| CatalogViewModelTest | Error offline y reintento sin estado viejo |
| RecoveryViewModelTest | Validación, un único correo y borrado del correo tras aceptación |
| RemoteRegistrationRepositoryTest | Ruta y payload M7 sin Bearer; 201 confirmado; errores y respuestas malformadas; sin reintentos |
| RegistrationViewModelTest | Validación posterior al envío, opcionales independientes, doble envío, cancelación, borrado de password, Retry-After y política M7 12–128 |
| RetryAfterTest | Segundos, fecha HTTP, valor vencido e inválido |

Solo src/test contiene respuestas sintéticas. MockWebServer usa HTTP local o TLS y CA propia
exclusivamente de tests; no hay trust-all ni certificados de prueba en producción.
No se hicieron pruebas de carga ni se promete capacidad concurrente del backend.

## Verificación física y estado del backend

NativeUiTest ejecutó **seis casos: cinco aprobados, uno omitido, cero fallos/errores**, en Xiaomi 2201122G
con Android 15: validación de acceso, registro y consentimientos sin enviar datos, selección de apariencia
con recreación de Activity y retorno a Sistema, política de cleartext por host y carga real
del catálogo público. Se omite el caso de reintento sin configuración porque este build
tiene Gateway real. La prueba real usa gatewaySmoke=true; no se ejecuta por defecto en CI.
Se superó el rechazo inicial de instalación
USB cuando el propietario habilitó la opción correspondiente. El harness pone únicamente
nuestra app en primer plano mediante am start antes de la regla Compose para evitar
el bloqueo OEM al lanzamiento desde segundo plano; no se modificaron ajustes del teléfono.

Sistema/Claro/Oscuro se guarda en DataStore. Se probaron selección, lectura de modo
guardado, seguimiento de Sistema, fallos de almacenamiento y fallback de preferencias
desconocidas. Dos pruebas comprueban los pares de contraste reales de ambas paletas.
CatalogFiltersTest verifica búsqueda local sin distinción de tildes/caso y filtro principal.
Capturas reales de los dos temas revisadas en [NATIVE_REDESIGN.md](../design/NATIVE_REDESIGN.md).
Esto no sustituye una auditoría TalkBack ni pruebas de escalado en múltiples dispositivos.

El OpenAPI público facilitado devuelve paths/components vacíos y declara nueve servicios
no disponibles. Sin embargo, los GET públicos de categorías (seis, nombres es/en) y
búsqueda (items vacíos, total y cursor_siguiente) devolvieron 200. El OpenAPI no permite
inferir que esos servicios estén caídos. HTTPS agotó el tiempo de conexión; el propietario
solicitó usar HTTP el 2026-11-18. Se configuró el origen recibido en local.properties
ignorado y Android cargó las seis categorías reales por HTTP. Quedan pendientes contratos
publicados y acceso autorizado para flujos privados; no se enviaron credenciales reales.
Discrepancias originales y próximos pasos están en PHASE_0.md y API_INVENTORY.md.

## Evidencia del cambio de transporte

El build genera res/xml/gateway_network_security.xml bajo build/ a partir del origen
local. base-config niega cleartext; domain-config lo permite solo si el origen usa HTTP
y para su host exacto sin subdominios. La prueba física consultó NetworkSecurityPolicy
y comprobó que el host configurado permite HTTP y unrelated.invalid no lo permite.
GatewayBoundaryInterceptor conserva esquema/host/puerto/rutas y no sigue redirecciones.
HTTPS mantiene las CA del sistema; no se introdujeron excepciones de certificados.

Captura del catálogo real revisada: [Explorar conectado](../design/device-explore-connected.png).
Sin backend modificado ni tokens enviados en esta verificación. HTTP no cifra datos en tránsito.

## Registro y verificación final — 2026-11-18

El propietario señaló que faltaba Crear cuenta. Se resolvió D03 con la referencia
M7 95f1d32c5bebb95ea9fbd864330275b4994e05e1, conservando Gateway y web de solo lectura.
RegistrationApi/RemoteRegistrationRepository usa POST /api/v1/identidad/cuentas, tipo
TURISTA y consentimientos {finalidad,otorgado}. No envía versión de política inventada,
no crea sesión y confirma únicamente 201 con mensaje no vacío. No consulta directamente M7.

Crear cuenta es visible antes de los campos de acceso. El formulario es nativo,
con NewUsername/EmailAddress y NewPassword, visor accesible, reglas y mensajes
asociados semánticamente a campos; tratamiento obligatorio, opcionales desactivados.
Password solo en memoria y borrado al terminar; ninguna persistencia de credenciales.
La pantalla confirmada explica abrir el enlace de correo y volver a iniciar sesión.
App Links y reenvío nativos aún no se implementaron.

Verificación final: verify-android.sh BUILD SUCCESSFUL, 44 JVM aprobadas, lint/formato
aprobados. connectedDebugAndroidTest con gatewaySmoke=true: seis casos, cinco
aprobados y uno omitido por requerir un build sin servidor; cero fallos/errores.
El primer intento fue bloqueado por INSTALL_FAILED_USER_RESTRICTED antes de ejecutar
pruebas; tras la aceptación USB del propietario, la suite terminó correctamente.

Se revisaron las capturas claras/oscuras del registro: campos, títulos y casillas
legibles, sin texto cortado; formulario desplazable y navegación de vuelta aprobada.
[NATIVE_REGISTRATION.md](../design/NATIVE_REGISTRATION.md) contiene plan y capturas.
No se crearon cuentas, no se enviaron correos de registro ni se probaron credenciales
reales. La entrega/activación del correo depende del despliegue M7/M10 y requiere
la prueba manual del propietario. No se declara ese recorrido remoto como aprobado.

## Evidencia física vigente

Diez casos, nueve aprobados y uno omitido con gatewaySmoke=true. Se repitió
tras corregir el contenedor Surface exclusivo de dos tests visuales; ambos cortes
aprobaron. [Capturas y alcance](../design/NATIVE_FLOW_VERIFICATION.md).
La prueba adicional del catálogo publicado compiló con ktlint aprobado; al ejecutar
no había dispositivo, por lo que no añade un caso aprobado.
APK final instalado (adb install -r: Success) y abierto (am start: Status ok).

Artefactos regenerados tras normalizar el espacio final de la licencia empaquetada:
assembleDebug, assembleRelease y bundleRelease BUILD SUCCESSFUL. No cambió la
lógica de la versión probada físicamente. La app instalada antes de desconectar
USB corresponde al corte funcional aprobado; las sumas de arriba son las del
artefacto regenerado. Wrapper Windows normalizado en Git mediante .gitattributes,
con CRLF en checkout. git diff --cached --check aprobado.

## Cierre vigente de la prueba de publicaciones

El propietario reconectó USB y aceptó instalar ambos APK. Ejecución directa del
runner AndroidJUnitRunner con gatewaySmoke=true y clase NativeUiTest: once casos,
diez status=0 aprobados, uno status=-4 (AssumptionViolatedException por origen
configurado), cero fallos; salida «OK (11 tests)». Esto sustituye el bloqueo USB
y los estados de prueba pendiente anotados arriba. Dos intentos previos fallaron
por esperar nodos fuera de la composición LazyColumn; se corrigió la espera por
título real y performScrollToNode antes de pulsar la acción. No era fallo de API.
El resumen auxiliar inicial contaba solo -3 como omitido; se corrigió al comprobar
el status -4 del runner, sin repetir pruebas exitosas ni ocultar un fallo de app.

Capturas reales revisadas en NATIVE_FLOW_VERIFICATION.md. La prueba abrió ficha
publicada, leyó descripción y llegó al acceso de reserva; no inició sesión ni
consultó ofertas privadas. App debug conservada instalada y MainActivity abierta.

CI inicial falló al instalar el paquete tools retirado; se configuró platform-tools.
El runner compartido también provocó OldTargetApi con plataformas preinstaladas
más nuevas. CI ahora instala API36/BuildTools35 en SDK aislado bajo runner.temp,
igual que el entorno local, manteniendo lint activo, sin baseline ni supresión.
La verificación remota de esa corrección sigue pendiente de resultado.
