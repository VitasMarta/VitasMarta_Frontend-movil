# VitasMarta — Android nativo

Proyecto Kotlin/Jetpack Compose con Clean Architecture, MVVM y paquetes por feature.
Flujo turista implementado: catálogo, reservas, perfil y recomendaciones.
Verificación local aprobada; diez pruebas físicas aprobadas; limitaciones abajo.

## Implementado y verificado localmente

- Diseño nativo propio con Poppins, identidad sierra/mar, navegación y recursos es-CO/en-US.
- Apariencia Sistema/Claro/Oscuro guardada en DataStore; selector de paleta disponible sin login.
- Búsqueda local de categorías por texto y filtro de categorías principales.
- Hilt, Retrofit/OkHttp, frontera de origen HTTP/HTTPS, errores tipados, cancelación y Retry-After.
- Categorías públicas: DTO, repositorio, ViewModel, carga, vacío, error y reintento.
- Login/logout, tokens solo en memoria, refresh single-flight y protección ante respuestas tardías.
- Registro de turista con Crear cuenta, validación y consentimientos independientes; confirmación por correo antes de acceder.
- Recuperación inicial por correo con confirmación anti-enumeración.
- Búsqueda remota por categoría/texto/precio, orden y cursor; ficha y franjas reales.
- Reservas: ofertas, política y aceptación, creación idempotente, historial, detalle, modificación, cancelación y PDF.
- Perfil: nombre visible, idioma nativo, intereses, consentimientos, derechos ARCO y exportación PDF.
- Recomendaciones M6 con transparencia IA, respuesta degradada 206 y alternativas desde el contenido recibido.
- 104 pruebas JVM/MockWebServer aprobadas; compilaciones debug/release, lint y formato aprobados.
- Diez pruebas instrumentadas aprobadas en Android 15 físico, incluida carga real del Gateway;
  un caso de catálogo sin configuración se omite en este build conectado.

Las categorías ya abren resultados y las pestañas privadas consultan sus servicios tras iniciar sesión.
La revisión posterior confirmó tres contenidos publicados, incluido el Buffet mediterráneo
semanal (Hilton Garden Inn). Se accede desde Buscar atractivos o Gastronomía.
Las ofertas reservables se consultan en M3 con sesión. Los tests no crean
reservas ni modifican cuentas reales automáticamente.
El propietario autorizó HTTP el 2026-11-18; la app permite ese transporte únicamente
al host del Gateway configurado. HTTP no cifra contraseñas ni tokens. HTTPS mantiene
su validación normal. La dirección del despliegue se configura en local.properties
(ignorado); no está embebida en las fuentes. El OpenAPI sigue sin publicar contratos.
La app no incluye datos simulados de producción.
El registro se confirmó contra el código M7 y su navegación/validación se probó en el teléfono.
La creación real y entrega del correo están pendientes de la prueba manual del propietario.
Restablecimiento y MFA siguen pendientes de integrar.
No hay persistencia de tokens ni restauración de sesión al reiniciar el proceso.

## Compilar y probar

Configurar JDK 17 y SDK API 36/Build Tools 35 en Android Studio o `local.properties`.
La URL HTTP o HTTPS del Gateway se configura localmente; no se inventa un valor por defecto.

```sh
./scripts/verify-android.sh
```

El script ejecuta assembleDebug, testDebugUnitTest, lintDebug, ktlintCheck y
assembleDebugAndroidTest. Formato por separado: `./gradlew :app:ktlintFormat`.
Con dispositivo autorizado: `./gradlew :app:connectedDebugAndroidTest`.

APK debug: `app/build/outputs/apk/debug/app-debug.apk` (generado, ignorado por Git).
APK release sin firma y AAB generados con R8. Release exige URL explícita;
firma con clave de distribución y publicación quedan pendientes.

## Documentación

- [Reglas](AGENTS.md), [arquitectura](doc/ARCHITECTURE.md) y [guía de desarrollo](doc/DEVELOPMENT.md).
- [Progreso y bloqueos](doc/specs/TASKS.md), [evidencia de implementación](doc/specs/IMPLEMENTATION.md).
- [Descubrimiento](doc/specs/PHASE_0.md), [inventario API](doc/specs/API_INVENTORY.md),
  [matriz de pantallas](doc/specs/SCREEN_MATRIX.md).
- [Diseño móvil propio](doc/MOBILE_DESIGN.md) y [rediseño y capturas reales](doc/design/NATIVE_REDESIGN.md).

La documentación original se conserva en `doc/` y `doc/specs/`. Gateway y web son
referencias de solo lectura, fijadas por commit en PHASE_0.md. El diseño web aporta
identidad y comportamiento; la app tiene composición e interacción nativas.

Estado remoto actualizado: [revisión de main y publicaciones](doc/specs/REPOSITORY_RECHECK_2026-11-18.md).
Once casos físicos ejecutados: diez aprobados y uno omitido. El recorrido público
listado publicado → ficha → acceso contextual a reserva fue aprobado en el teléfono.
PR de revisión: https://github.com/VitasMarta/VitasMarta_Frontend-movil/pull/1 (sin merge).
