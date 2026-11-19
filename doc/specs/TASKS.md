# Plan de implementación

Estado al 2026-11-18: flujo turista de Fases 2–4 implementado y activado en orden tras verificar cada corte. 104 pruebas JVM aprobadas, lint sin errores/advertencias, formato aprobado; APK debug, APK release sin firma y AAB compilados. Doce casos físicos actuales: once aprobados y uno omitido; incluye letra200% y navegación de catálogo. Quedan pendientes identidad avanzada, comprobación remota de operaciones privadas, accesibilidad completa y firma de distribución. Los registros inferiores son históricos; este estado y [IMPLEMENTATION.md](IMPLEMENTATION.md) prevalecen.

## Fase 0 — Descubrimiento y diseño (bloqueante)
- [x] Leer AGENTS y skills; registrar nombres/rutas/versiones. Evidencia: [PHASE_0.md](PHASE_0.md), frontend-design 1.1.1 y modern-web-guidance 1.0.1; ambas leídas y aplicadas a la propuesta.
- [x] Inspeccionar web/móvil y Gateway; fijar commits. Referencias descargadas para lectura dentro de `.git/vitasmarta-references/` porque no se localizaron clones hermanos. Rutas/filtros/auth/ambientes y discrepancias: [PHASE_0.md](PHASE_0.md).
- [ ] Confirmar alcance por rol, namespace, SDK, ambientes, URL real y acceso de pruebas.
- [ ] Completar API_INVENTORY.md y matriz de pantallas/roles/endpoints. Inventario estático y matriz creados; ratificación de DTO y pruebas reales pendientes (F0-02/F0-03).
- [x] Extraer tokens y crear propuesta de navegación; validar identidad contra las fuentes inspeccionadas. [MOBILE_DESIGN.md](../MOBILE_DESIGN.md), tokens auditables y lámina propia. Rediseño nativo revisado en capturas del dispositivo, en claro y oscuro; ver NATIVE_REDESIGN.md.

## Fase 1 — Base técnica
- [x] Crear proyecto, Gradle wrapper, catálogo y CI. Wrapper con SHA-256, versiones fijas; CI configurada, no ejecutada remotamente.
- [x] Configurar theme, navegación, DI, cliente HTTP/HTTPS, errores y recursos. Sin origen ficticio ni excepciones TLS; es-CO/en-US y Poppins con licencia.
- [x] Compilar y ejecutar lint/unitarias. BUILD SUCCESSFUL; detalle de comandos y 104 pruebas en IMPLEMENTATION.md.

## Fase 2 — Identidad y catálogo
- [ ] Completar sesión y formularios de identidad.
  - [x] Login/logout con tokens en memoria, refresh single-flight y descarte tras logout.
  - [x] Recuperación inicial por correo con respuesta anti-enumeración y prevención de doble envío.
  - [x] Registro nativo: acceso Crear cuenta, DTO confirmado en M7, consentimientos y confirmación por correo; repositorio/ViewModel probados. Navegación/validación física aprobada; creación y correo reales quedan para prueba manual del propietario.
  - [ ] Restablecimiento, MFA y OAuth: pendientes de contratos/callbacks del flujo nativo.
  - [ ] Persistencia y restauración de sesión: no implementadas; requerirían Keystore y pruebas propias.
- [x] Completar catálogo, filtros, cursor, detalles y franjas.
  - [x] Categorías públicas con DTO, mapper/repositorio, ViewModel y UI de estados; búsqueda local por nombre/slug y filtro de principales.
  - [x] Búsqueda, filtros, cursor, ficha y franjas con DTO de M2 c71ad902; tests deterministas aprobados. El despliegue auditado devuelve cero atractivos: no se declara prueba remota de ficha/reserva exitosa.
- [ ] Completar pruebas de UI del alcance total. 104 JVM/MockWebServer aprobadas; once casos físicos: diez aprobados y uno omitido, incluido el recorrido publicado/ficha/acceso contextual. No equivale a prueba remota de mutaciones privadas.

## Fase 3 — Reservas
- [x] Integrar flujo de reserva, idempotencia y resolución de conflictos.
- [x] Historial, detalle y cancelación si está documentada.
- [ ] Smoke test en ambiente autorizado (no generar reservas reales sin permiso).

## Fase 4 — Perfil y recomendaciones
- [x] Perfil, idioma, consentimientos y derechos según alcance.
- [x] Recomendaciones, fallback 206 y alternativas.
- [ ] Verificar permisos, navegación por roles y UX.

## Fase 5 — Entrega
- [ ] Revisar accesibilidad, dispositivos, estados, seguridad y privacidad.
- [ ] Ejecutar build, tests, lint, formato e instrumentadas disponibles.
- [ ] Firmar APK/AAB con claves de distribución autorizadas. APK release sin firma y AAB compilados correctamente con R8.
- [ ] Documentar instalación, configuración, limitaciones y publicación Play.

## Fase posterior — otros roles
- [ ] Aprobar alcance prestador, administración y monitoreo; crear specs propias.

## Registro de bloqueos y decisiones
| Fecha | ID | Bloqueo/decisión | Evidencia | Próximo paso |
|---|---|---|---|---|
| 2026-11-17 | F0-01 | Alcance por rol, namespace y minSdk propuestos pendientes de confirmación | Consulta enviada al propietario; REQUIREMENTS mantiene alcance provisional | Confirmar turista primero, co.vitasmarta.mobile y API 26 |
| 2026-11-18 | F0-02 parcial | HTTP autorizado y configurado; OpenAPI sigue sin publicar contratos | GET públicos disponibles; restricción de HTTPS sustituida por instrucción explícita del propietario | Ratificar contratos restantes y acceso de prueba para identidad |
| 2026-11-17 | F0-03 | DTO divergentes entre contrato escrito y código web | D01–D11 en PHASE_0; filas B en API_INVENTORY | Contrastar OpenAPI real de M1 y contratos downstream |
| 2026-11-17 | F0-04 resuelto | SDK y proyecto preparados localmente | API 36, Build Tools 35, JDK 17, Gradle 8.13 y AGP 8.13.2; verificaciones aprobadas | Mantener versiones y evidencia en IMPLEMENTATION.md |


## Registro de ejecución — Fase 0 (2026-11-17)

- Base móvil `d49342fb9bfcfb0beb831966e513e787d1b44dbf`; rama `feature/phase-0-discovery`.
- Gateway `fb973278da8b455e9854dc0f76d5546af062fab6`; web `041dce2a2299262eae23c157f1e3d686e314aad8`.
- Lectura completa de documentación local, skills y fuentes contractuales relevantes: evidencia en PHASE_0.md.
- modern-web-guidance search/retrieve ejecutados correctamente; guías accessibility/forms leídas completas.
- Entregables: PHASE_0.md, API_INVENTORY.md, SCREEN_MATRIX.md, MOBILE_DESIGN.md,
  design/mobile-tokens.json y design/mobile-direction.svg. README/AGENTS raíz enlazan documentación existente.
- No se inició Fase 1: respeta el descubrimiento bloqueante. No hay tests Android ni APK/AAB.
- Validaciones documentales finales ejecutadas:
  - `git diff --check`: éxito (archivos versionados); comprobación adicional de espacios en entregables nuevos: éxito.
  - Script Python stdlib: enlaces locales de entregables válidos; SVG XML válido;
    JSON de tokens válido; 39 operaciones API con IDs únicos; tablas de inventario/matriz consistentes.
  - Contraste sRGB: tinta/base 13.11:1, tinta/niebla 10.74:1, blanco/tinta 13.53:1,
    esmeraldaTexto/base 6.41:1, ámbar/base 4.87:1, coral/base 6.27:1; todos >= 4.5:1.
    Solo verifica pares declarados; no certifica accesibilidad de una pantalla real.
  - `rsvg-convert --output doc/design/mobile-direction.png doc/design/mobile-direction.svg`:
    éxito; PNG abierto y revisado visualmente. Lámina legible sin texto cortado ni superpuesto;
    placeholders explícitos. Revisión de propuesta, no ejecución de la app ni de la web.
  - `git status --porcelain` en ambas referencias: vacío; no se modificaron.
  - `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug`:
    salida 127, `./gradlew: No such file or directory`. Build, unitarias y lint **no ejecutados**.
- Archivos del propietario en `doc/` permanecen sin seguimiento como al inicio;
  se añadieron notas/progreso sin moverlos. No se hizo commit, push ni publicación.


## Implementación autorizada sin despliegue — en curso

El propietario indicó: “ve implementando mientras consigo la url del apigateway”.
Se autoriza avanzar en la base técnica y pruebas locales sin cerrar artificialmente
Fase 0. Luego facilitó una URL HTTP: el OpenAPI devolvió 200 pero sin rutas ni
componentes y con nueve servicios no disponibles; HTTPS agotó 5 s de conexión.
No se configura esa dirección en la app ni se envían credenciales por HTTP.

- Proyecto Kotlin/Compose, wrapper, catálogo de versiones, Hilt, cliente HTTPS,
  errores, recursos bilingües, fuente Poppins y CI creados; verificación en curso.
- SDK API 36, Build Tools 35 y platform-tools instalados localmente con licencias.
- Primera compilación y pruebas: pendientes de resultado, no completadas aún.
- Detalles de configuración y alcance provisional: [DEVELOPMENT.md](../DEVELOPMENT.md).


## Resultado de implementación local

- Base completada y verificada antes de iniciar los cortes de Fase 2.
- Categorías, login/logout/refresh y recuperación inicial verificados con pruebas deterministas.
- Resultado final: 26 tests, 0 fallos, 0 errores, 0 omitidos. Lint: 0 errores y 0 warnings;
  avisos de actualización se conservan como información, sin baseline ni supresión de defectos.
- `./scripts/verify-android.sh`: BUILD SUCCESSFUL. Incluye compilación del APK instrumentado.
- Pruebas negativas: release sin URL y origen HTTP son rechazados como se esperaba.
- Inspección del APK: API 26/36, INTERNET, cleartext y backup desactivados, licencia empaquetada;
  sin assets mock ni archivos .env. APK SHA-256 en IMPLEMENTATION.md.
- `adb devices`: sin dispositivo en la última comprobación. El propietario está preparando USB;
  no se dan las pruebas de interfaz ni la revisión visual de la app por ejecutadas.
- No se iniciaron reservas ni llamadas autenticadas al despliegue recibido. No se modificaron
  fuentes de Gateway/web, no se hizo commit/push ni se publicó una release.


## Intento de pruebas físicas por USB

El propietario conectó el teléfono y habilitó depuración USB. ADB lo detecta autorizado
(modelo Xiaomi 2201122G; no se registra identificador del dispositivo).
`./scripts/verify-android.sh :app:connectedDebugAndroidTest` terminó con
INSTALL_FAILED_USER_RESTRICTED (instalación cancelada/restringida por el teléfono),
0 pruebas ejecutadas. Pendiente habilitar “Instalar por USB”/aceptar la instalación;
no se marca UI aprobada ni se modifica la configuración de seguridad del teléfono.

## Rediseño nativo y tema oscuro — verificación completada

- frontend-design: `/home/juane/.claude/plugins/marketplaces/claude-plugins-official/plugins/frontend-design/skills/frontend-design/SKILL.md`, 1.1.1.
- modern-web-guidance: `/home/juane/.codex/plugins/cache/googlechrome/modern-web-guidance/1.0.1/skills/modern-web-guidance/SKILL.md`; guías completas dark-mode, dark-mode-toggle y color, adaptadas a Compose.
- [x] Dirección propia sierra/mar, Poppins, navegación, estados de conexión y formularios nativos.
- [x] Sistema/Claro/Oscuro en DataStore; selección accesible, barras y tema global coherentes.
- [x] Contrastes de las dos paletas: texto >=4.5:1 y límites de controles >=3:1 en pares comprobados.
- [x] `verify-android.sh`: build, 34 unitarias, lint y formato aprobados.
- [x] `:app:connectedDebugAndroidTest`: tres pruebas físicas aprobadas, cero fallos. Se resolvió el bloqueo USB y el arranque del harness en segundo plano mediante foreground de nuestra propia app; sin modificar ajustes de seguridad del teléfono.
- [x] Capturas claras/oscuras revisadas; evidencia en [NATIVE_REDESIGN.md](../design/NATIVE_REDESIGN.md).

Estos resultados sustituyen los estados pendientes históricos de USB registrados arriba.
La selección de apariencia persiste; los tokens de sesión siguen solo en memoria.
No se da por completo el MVP ni la integración HTTPS del Gateway.

## Cambio a HTTP solicitado por el propietario — 2026-11-18

La instrucción explícita sustituye el requisito anterior de HTTPS exclusivo, también
para release. No modifica el backend ni los contratos. La URL recibida queda solo
en local.properties ignorado; no se agrega a fuentes, ejemplos o documentación.

- [x] Validación de build y runtime acepta HTTP/HTTPS sin credenciales/prefijo/query/fragmento.
- [x] Política Android generada habilita HTTP solo para el host configurado, sin subdominios.
- [x] Interceptor conserva esquema, host, puerto y /api/v1/; sin redirecciones ni bypass TLS.
- [x] Build, 36 unitarias, lint y formato aprobados.
- [x] Verificación física de política y carga pública desde el Gateway: cuatro instrumentadas aprobadas, cero fallos; un caso sin configuración omitido por tener servidor real.
- [x] Captura revisada: seis categorías reales visibles. APK reinstalado para pruebas manuales.
- [x] :app:preReleaseBuild acepta el origen HTTP; no se firma ni publica release.

Los registros anteriores describen resultados históricos bajo la política previa.
HTTP deja las credenciales y los datos sin cifrar en tránsito; el propietario ha
solicitado este cambio tras recibir esa explicación. No se han enviado credenciales reales.

## Registro solicitado — 2026-11-18

- Causa: no existía acceso de registro móvil; D03 impedía elegir entre payloads web divergentes.
- Fuente adicional de solo lectura: servicio_identidad 95f1d32c5bebb95ea9fbd864330275b4994e05e1; CuentaController, RegistroRequest, ConsentimientoRequest, PoliticaPassword y snake_case. D03 resuelta.
- [x] Plan visual y crítica antes del código: [NATIVE_REGISTRATION.md](../design/NATIVE_REGISTRATION.md).
- [x] Skills frontend-design 1.1.1 y modern-web-guidance 1.0.1 aplicadas; forms, validate-input-after-interaction y autofill-sign-up-form recuperadas y leídas completas (984 líneas).
- [x] Crear cuenta visible antes de los campos de acceso desde Perfil y otros destinos privados.
- [x] Ruta POST pública, solo TURISTA, consentimientos independientes desactivados inicialmente y tratamiento obligatorio.
- [x] Contraseña 12–128 caracteres, visor, semántica de autocompletado nuevo, validación al enviar, foco y teclado.
- [x] No duplicar envíos; respetar Retry-After; borrar password al finalizar; no almacenar credenciales ni crear sesión tras registro.
- [x] Confirmación solo con 201 válido; instrucciones de verificar correo y volver a acceso.
- [x] Verificación final tras ajustes accesibles: 44 pruebas JVM, compilación, lint y formato aprobados.
- [x] Nueva prueba física y capturas claras/oscuras: tras aceptar USB, seis casos, cinco aprobados, uno omitido (requiere origen vacío), cero fallos. Primer intento bloqueado antes de ejecutar tests; resuelto en la repetición.

No se envía ningún registro real automáticamente. Creación real, envío/entrega del correo
y apertura del enlace dependen del despliegue M7/M10 y se dejan a la prueba manual del propietario.

## Auditoría de los 12 repositorios — 2026-11-18

El propietario comprobó login real y señaló que Reservas/Para ti no permiten operar y
las categorías no navegan. Se confirmó que la causa principal es código móvil aún
no implementado: ServiceMessage fijo y filas sin acción, no un error HTTP de M3/M6.
Informe con commits, respuestas remotas y estado de cada repositorio:
[REPOSITORY_AUDIT_2026-11-18.md](REPOSITORY_AUDIT_2026-11-18.md).

- [x] Consultar main actual y PR/Actions en los 12 repositorios; fuentes conservadas de solo lectura.
- [x] Inspeccionar DTO/rutas reales de M2, M3 y M6; D09 y el esquema básico D06 ya son comprobables por fuente.
- [x] GET públicos: seis categorías y búsqueda sin filtros con total=0; OpenAPI sigue vacío.
- [x] Lecturas privadas anónimas: 401 esperado; no se usó la cuenta del propietario ni se extrajeron tokens.
- [x] Completar catálogo/ficha antes de reservas y recomendaciones, siguiendo las fases; compilación/pruebas aprobadas antes de activar el siguiente corte.
- [x] Integrar reservas y M6 con repositorios/ViewModels y pruebas propias; no atribuir a estos tests operaciones privadas reales.
- [x] Repetir diagnóstico con datos actuales: ahora hay tres contenidos públicos. La observación inicial de total=0 no acredita el estado actual; ver REPOSITORY_RECHECK_2026-11-18.md.
- [x] Retirar ServiceMessage fijo; categorías navegan y cada feature consulta su API y presenta sus estados reales.

La auditoría no cambió código ni produjo reservas reales. El login confirmado por
el propietario se registra como observación de uso, no como prueba automatizada nuestra.

## Integración final sin subagentes — 2026-11-18

A petición del propietario, la integración final continúa con un único agente.
Catálogo, reservas y perfil/recomendaciones se activaron secuencialmente, con
verify-android.sh aprobado en cada corte. Resultado final local: 104 tests, cero
fallos/errores/omitidos; lint cero errores y advertencias; ktlint aprobado.
:app:bundleRelease :app:assembleRelease terminó BUILD SUCCESSFUL, incluido R8.
No se proporcionó clave de distribución ni se publicó ningún artefacto.

Sesión incorpora generación para limpiar pantallas incluso al salir y entrar con
la misma cuenta; ningún resultado tardío puede reponer datos privados. PDF solo
en memoria durante selector nativo, conserva rotación y se descarta al cambiar
sesión. Idioma nativo usa LocaleManager desde API33 y fallback API26–32.

Bloqueos vigentes: OpenAPI sin rutas,
mutaciones privadas aún sin ensayo real, MFA/OAuth/restablecimiento nativo sin
cierre contractual y claves de distribución ausentes. No se modifica el backend.

## Cierre físico y revisión solicitada de publicaciones

- [x] Diez instrumentadas ejecutadas: nueve aprobadas y una omitida; cero fallos.
- [x] Revisar y guardar capturas oscuras de las dos pantallas con fixtures, claramente rotuladas.
- [x] Reinstalar APK debug y abrir MainActivity: Success / Status: ok.
- [x] Consultar main actualizado de los doce repositorios, sin editar sus fuentes.
- [x] Confirmar tres contenidos reales con búsqueda sin filtros y uno en gastronomico; fichas 200.
- [ ] Ejecutar nuevo caso físico de listado publicado → ficha → acceso a reserva: APK de prueba compilado, dispositivo desconectado; No connected devices, cero casos en ese intento.
- [ ] Verificar con sesión las ofertas M3 y el recorrido privado real cuando regrese el propietario.

La conclusión anterior de catálogo vacío queda sustituida por
[REPOSITORY_RECHECK_2026-11-18.md](REPOSITORY_RECHECK_2026-11-18.md).

## Autorización de entrega

El propietario autoriza fetch, pull, commits, push y PR; merge expresamente
excluido. También autoriza las acciones locales necesarias para esta tarea.
Se conservan cambios exclusivamente en el repositorio móvil y referencias de
otros repositorios sin edición. La autorización no cambia por sí sola la política
de sandbox de la herramienta. Se prepara commit y PR revisable con pendientes
remotos/USB explícitos; no se publicará una app en Play ni se hará merge.

## Cierre posterior tras reconectar el celular

- [x] Once casos físicos con AndroidJUnitRunner: diez aprobados, uno omitido y cero fallos.
- [x] Recorrido listado público publicado → ficha real → botón de reserva → acceso contextual aprobado.
- [x] Capturas reales revisadas y guardadas; app instalada y abierta para prueba manual.
- [x] Commit/push realizados y PR borrador [#1](https://github.com/VitasMarta/VitasMarta_Frontend-movil/pull/1) abierto; no merge.
- [x] Corregir instalación CI de paquete SDK tools retirado; platform-tools verificado remotamente.
- [ ] Cerrar CI con SDK aislado API36: ejecución remota pendiente de resultado.
- [ ] Verificar ofertas M3 con sesión y realizar transacción real únicamente cuando el propietario lo solicite de forma concreta.

Este corte sustituye el bloqueo USB y los pendientes del caso publicado en los
registros inmediatamente anteriores. Pruebas privadas reales, identidad avanzada,
accesibilidad exhaustiva y firma de distribución conservan sus pendientes.

## Rediseño fotográfico de Explorar — en curso

Rama feature/explore-visual-redesign sobre main 1ff9240 (PR1 fusionado por el
propietario). Autorizado: portada fotográfica, categorías visuales, fondos
claro/oscuro; sin cambios en backend/contratos. Plan y crítica antes del código:
[EXPLORE_VISUAL_REDESIGN.md](../design/EXPLORE_VISUAL_REDESIGN.md).

- [x] Skills frontend-design y modern-web-guidance, guías de imágenes y color.
- [x] Fotografías locales WebP con fuentes y licencias documentadas; 132KB.
- [x] Cabecera, ilustraciones/categorías, cuadrícula 1/2/3 columnas y textos sin truncar.
- [x] Build, 104 JVM sin fallos, lint y ktlint aprobados.
- [x] Doce casos físicos: once aprobados y uno omitido; capturas claro/oscuro y a320dp/font200% revisadas.
- [x] Evidencia final y PR2 publicados; ambos checks aprobados. Fusionado por el propietario, no por el agente.

Continuidad para otro agente: [HANDOFF.md](HANDOFF.md). Se actualiza durante el
trabajo, registra comandos, autorizaciones, estado y siguientes pasos sin secretos.

Verificación del rediseño cerrada localmente: 104 JVM, lint/ktlint y12 casos
físicos (11 aprobados/1 omitido). Primeras observaciones visuales corregidas y
suite repetida. App instalada y abierta. Continuidad actualizada en HANDOFF.md.
Publicación/CI del PR de esta rama: consultar GitHub; no confundirlo con PR1,
ya fusionado por el propietario y con ambos checks aprobados.

## Fotografías de categorías aportadas por el usuario

Rama `feature/category-photographs` sobre `51c4f59` (PR2 fusionado por el propietario).

- [x] Conservar los cuatro PNG originales e integrar sus WebP en Cultura,
  Aventura, Ecoturismo y Gastronomía (`gastronomico`).
- [x] Optimizar copias sin ampliar las fuentes, incorporar créditos y revisar
  recortes a anchos representativos de una, dos y tres columnas.
- [x] Compilar APK debug e instrumentado. 104 pruebas JVM sin fallos,
  lint y ktlint aprobados mediante `verify-android.sh`.
- [x] Ejecutar el runner físico y revisar la cuadrícula de dos columnas
  en claro y oscuro. Doce casos, once aprobados y uno omitido sin fallos.
- [ ] Publicar PR y verificar CI.

Asignación, encuadres y continuidad en [HANDOFF.md](HANDOFF.md).
