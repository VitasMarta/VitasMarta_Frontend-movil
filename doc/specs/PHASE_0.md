# Fase 0 — Descubrimiento verificable

Fecha: 2026-11-17 (America/Bogota). Rama: `feature/phase-0-discovery`.
Estado histórico del descubrimiento: cierre pendiente de contratos/entorno remoto.
Posteriormente el propietario autorizó avanzar sin URL; Fase 1 y parte de Fase 2 ya
están implementadas y verificadas localmente. Ver TASKS.md e IMPLEMENTATION.md.

## Fuentes y precedencia

1. Instrucción del propietario: Kotlin/Compose, mejora del diseño web, cambios solo en el móvil.
2. AGENTS y documentación local leídos completos: README.md, doc/README.md,
   doc/AGENTS.md, doc/ARCHITECTURE.md, doc/API.md, doc/DESIGN.md,
   doc/specs/REQUIREMENTS.md y doc/specs/TASKS.md.
3. Código/configuración del Gateway para exposición, autenticación, RBAC y comunicación.
4. Código web y contratos escritos para payloads y comportamiento; discrepancias se registran.
5. OpenAPI desplegado y smoke tests autorizados pendientes: ninguna inspección estática
   demuestra disponibilidad ni conformidad de un despliegue real.

| Repositorio | Commit inspeccionado | Ubicación relativa al móvil | Estado |
|---|---|---|---|
| Móvil | `d49342fb9bfcfb0beb831966e513e787d1b44dbf` | `.` | HEAD inicial solo contenía README.md; doc/ ya existía sin seguimiento |
| Gateway | `fb973278da8b455e9854dc0f76d5546af062fab6` | `.git/vitasmarta-references/servicio_api_gateway` | Descargado para inspección; sin modificaciones |
| Web | `041dce2a2299262eae23c157f1e3d686e314aad8` | `.git/vitasmarta-references/VitasMartaFrontend` | Descargado para inspección; sin modificaciones |

No se encontraron clones hermanos en las ubicaciones revisadas. Las referencias se
almacenaron dentro de `.git/` para respetar el alcance de escritura, sin añadir su código
al árbol entregable. Se pueden consultar en GitHub por los commits de esta tabla.

Fuentes principales del Gateway: `src/main/resources/application{,-common}.yml`,
`infrastructure/web/route/TablaRutasConfig.java`, `infrastructure/config/MatrizEnConfiguracion.java`,
`application/port/OrdenFiltrosBorde.java`, filtros `JwtBordeFilter`, `AutorizacionBordeFilter`,
`CorrelacionFilter`, `LocaleFilter`, `application/usecase/seguridad/AutorizarSolicitudUseCase.java`,
`domain/model/CodigoErrorBorde.java`, `infrastructure/security/ValidadorFirmaNimbus.java`,
`doc/contratos/CONTRATOS_M1_API_GATEWAY.md`, `render.yaml` y documentación de configuración.
Los nombres Java son relativos a `src/main/java/com/unimag/servicio_api_gateway/`.

Fuentes web: `src/core/api/{routes,types,transport,gatewayClient,bffServer,idempotency}.ts`,
`src/core/auth/{session,refresh,preferences}.ts`, `src/core/config/gateway.ts`,
`src/features/catalogo/{api,HomeView,cards}.tsx` (api es `.ts`),
`src/features/reservas/api.ts`, `src/features/auth/AuthViews.tsx`,
`src/features/perfil/{PerfilView.tsx,consentimientos.ts}`, `src/app/globals.css`,
`src/app/icon.svg`, `src/app/[locale]/layout.tsx`, `src/shared/ui/SiteChrome.tsx`,
`.env.example` y los dos documentos contractuales de la raíz web.

## Skills efectivamente leídas

| Skill real | Versión | Ruta | SHA-256 de SKILL.md |
|---|---|---|---|
| frontend-design | plugin 1.1.1 | `/home/juane/.claude/plugins/marketplaces/claude-plugins-official/plugins/frontend-design/skills/frontend-design/SKILL.md` | `d91970639e9f5c37682ac7ab60094d35f1c7c1f38d731bd56396563aee10c1d3` |
| modern-web-guidance | plugin 1.0.1; catálogo `2026_11_10-47b4c80e` | `/home/juane/.codex/plugins/cache/googlechrome/modern-web-guidance/1.0.1/skills/modern-web-guidance/SKILL.md` | `1292f0ce14794a5bc49af58e99d5f6d4edf10acd9472cade94ab0c5590892716` |

`front-desing` no apareció en la búsqueda; se encontró la variante `frontend-design`.
Se leyó la instalación existente de Claude sin instalar ni copiar otra skill.

Comandos ejecutados correctamente:

```sh
npx -y modern-web-guidance@latest search "accessible mobile navigation" --skill-version 2026_11_10-47b4c80e
npx -y modern-web-guidance@latest retrieve "accessibility,forms"
```

La recuperación produjo `/tmp/mwg-guide-274591-1794969756463.md` (1005 líneas), leído
completo por rangos. Se aplican sus principios a la propuesta de diseño, no sus APIs
HTML/CSS/DOM. Adaptaciones y citas específicas en MOBILE_DESIGN.md. No se aplica
Baseline de navegadores a Android ni se añade WebView, React o JavaScript a la app.

## Hallazgos de integración

- Los nueve dominios de negocio se exponen bajo `/api/v1/`; `/estado` es propio del borde.
  TablaRutasConfig conserva la ruta tal cual, sin RewritePath. No añadir `/bff` en Android.
- Cadena del borde: correlación → filtrado de entrada → locale → JWT → autorización →
  rate limit → circuito → medición/auditoría. La matriz 1.2.0 aplica primera coincidencia.
- GET de catálogo y buenas prácticas es público. Login, registro, MFA, refresh y recuperación
  tienen entradas públicas explícitas. Resto sin regla especial exige credenciales;
  las restricciones del servicio de dominio siguen vigentes. Las rutas `interno`/`internal`
  y estado-sostenibilidad no son operaciones de usuario.
- El borde valida ES256 con JWKS de M7 y reenvía el Bearer íntegro. Los claims son `iss`,
  `sub`, `rol`, `prestador_ref` cuando aplica, `jti`, `iat`, `exp`. La UI nunca autoriza al servidor.
- Cabeceras: UUID por intención en X-Correlation-Id; Accept-Language es-CO/en-US.
  X-User-Locale representa preferencia explícita, superior a Accept-Language; leer
  X-Resolved-Locale. M3 emite claves de idempotencia; no fabricarlas en Android.
- El BFF web oculta refresh_token en cookie httpOnly y devuelve access_token. Android debe
  hablar directamente a M1 y manejar el par de tokens. Memoria para access y cifrado Keystore
  para refresh persistente; rotación atómica y single-flight, sin recursión ni resurrección tras logout.
- `application.yml`: perfiles dev/prod; destino por variables de despliegue; `render.yaml`
  no establece un dominio HTTPS público verificado. `.env.example` web solo propone desarrollo.
  Ninguna URL de microservicio se reutiliza como API base móvil.

## Discrepancias y decisiones de implementación

| ID | Evidencia | Decisión / pendiente |
|---|---|---|
| D01 | Web api.ts usa `auth:false` para alternativas; M1 no las declara públicas | En móvil exigir sesión; no modificar Gateway ni saltarlo |
| D02 | Contrato/guía web reserva desde franjas M2; reservas/api.ts obtiene ofertas M3 | Disponibilidad informativa M2; reserva solo con oferta, tarifa y política de M3. Confirmar DTO contra OpenAPI real |
| D03 resuelta 2026-11-18 | Registro escrito usa `aceptado/version_terminos`; AuthViews usa `otorgado` | RegistroRequest/ConsentimientoRequest de M7 95f1d32c confirman `otorgado`, snake_case y versión fijada por M7; registro móvil implementado |
| D04 | Reset escrito usa `nueva_password`; AuthViews envía `password` | Recuperación inicial documentada; reset pendiente de esquema real |
| D05 | MFA escrito usa `codigo_totp`; session.ts envía `{mfa_token,codigo}` | Confirmar desafío/esquema antes de habilitar MFA; otros roles siguen fuera del alcance aprobado |
| D06 | Recomendaciones escritas tienen `items/estado_respuesta`; api.ts adapta `recomendaciones/modo/degradada_fallback` | Confirmar esquema M6; conservar 206 como éxito degradado; no fabricar ocupación 0 ni fecha actual |
| D07 | Guía web prohíbe cards; globals.css/cards.tsx las implementan y añaden azul de acción | Propuesta móvil propia: jerarquía táctil y superficies continuas, identidad original; sin copiar plantillas desktop |
| D08 | Web comenta reescritura M4 desde `/v1`; TablaRutasConfig no reescribe | Usar solo rutas expuestas M1; cualquier trabajo M4 requiere contrastar el servicio real |
| D09 | Web types.ts declara supuestos en resultados M2, política y franja_ref | No convertir esos supuestos en contrato Android; validar esquema de catálogo y ofertas |
| D10 | Perfil web edita nombre/apellido/telefono/pais_residencia; documento dice solo “campos del perfil” | Confirmar lista de escritura del titular; no reenviar indiscriminadamente todos los campos GET |
| D11 | ARCO CANCELACION está documentado; no se ha demostrado eliminación de cuenta inmediata | Diseñar seguimiento ARCO, sin inventar DELETE /actual ni prometer borrado instantáneo |

## Decisiones propuestas y bloqueos abiertos

| ID | Estado | Qué falta | Cómo desbloquear |
|---|---|---|---|
| F0-01 | Pendiente de propietario | Turista primero, prestadores después; namespace `co.vitasmarta.mobile`, minSdk 26 | Responder consulta enviada en esta sesión; no excluir definitivamente otros roles sin decisión |
| F0-02 | Bloqueante | URL HTTPS autorizada, ambiente y acceso de prueba | Proporcionar URL y mecanismo seguro de credenciales; no pegar tokens/contraseñas en documentos |
| F0-03 | Bloqueante por funcionalidad | OpenAPI agregado real y discrepancias D02–D06, D09–D10 | Inspeccionar `/api-docs` de M1 autorizado y contrastar payloads; sin inventar esquemas |
| F0-04 | Preparación técnica pendiente | SDK, sdkmanager, adb y emulador no localizados; sin wrapper/proyecto | Preparar toolchain reproducible dentro del móvil en Fase 1, con versiones compatibles y descargas autorizadas |
| F0-05 | Pendiente de revisión | Propuesta de identidad y navegación móvil | Revisar MOBILE_DESIGN.md y lámina SVG; aún no validada en dispositivo |
| F0-06 | Posterior, fuera del MVP provisional | OAuth/App Links, política de callbacks nativos, eliminación de cuenta y firma release | Confirmar soporte; nunca capturar OAuth en WebView ni inventar callbacks/endpoints |

No se solicita permiso para modificar el backend. Ningún bloqueo se resuelve con mocks productivos.

## Evidencia de herramientas y comprobaciones

| Comando/inspección | Resultado observado el 2026-11-17 |
|---|---|
| `git status --short --branch` inicial | main...origin/main; doc/ sin seguimiento, conservado |
| `git rev-parse HEAD` en los tres clones | Commits de la tabla de referencias |
| `git switch -c feature/phase-0-discovery` | Rama creada |
| `java -version` | OpenJDK 21.0.12.1; también existe JDK 17 en /usr/lib/jvm |
| Búsqueda en PATH | gradle, sdkmanager, adb, emulator y kotlinc ausentes |
| Entorno SDK | ANDROID_HOME/ANDROID_SDK_ROOT no configuradas; ~/Android/Sdk no encontrado |
| Inspección del proyecto | Sin gradlew, build.gradle, settings.gradle, app/ ni SDK versionado |
| Búsqueda adicional | Sin herramientas SDK en Emuladores, Applications, /opt y /usr/local (profundidad revisada 5) |

La ausencia en estas ubicaciones no prueba que no exista otro SDK fuera de ellas.
No se arrancó ni compiló la web/Gateway ni se ejecutaron sus tests: referencias de solo lectura.
Las comprobaciones documentales finales están registradas en TASKS.md: enlaces,
XML/JSON, tablas, 39 IDs únicos, seis pares de contraste >=4.5:1 y revisión del PNG
renderizado. El intento de Gradle finalizó en salida 127 por ausencia de wrapper;
no se ejecutaron build, unitarias ni lint Android. Ambas referencias siguen limpias.

## Criterio para avanzar

Cerrar la decisión F0-01 y acceso F0-02; contrastar los esquemas que se implementarán
con F0-03. Preparar SDK y versiones en la base técnica. Solo marcar la Fase 1 completa
cuando assembleDebug, testDebugUnitTest, lintDebug y formato pasen realmente. Las siguientes
fases conservan el orden de TASKS.md; no se ha implementado ni validado una funcionalidad Android.

## Decisión posterior de transporte — 2026-11-18

El propietario solicitó HTTP explícitamente. La falta de HTTPS deja de bloquear la
conexión móvil; el origen se configura localmente y permite el catálogo real. Permanecen
pendientes la publicación de contratos y las credenciales de prueba de flujos privados.
La política y los resultados vigentes están en IMPLEMENTATION.md y TASKS.md.

## Resolución del registro — 2026-11-18

Referencia de lectura adicional: servicio_identidad, commit
95f1d32c5bebb95ea9fbd864330275b4994e05e1. El propietario señaló la ausencia de
Crear cuenta. La inspección de CuentaController y sus DTO/caso de uso resuelve D03
sin deducir endpoints ni cambiar M1/M7. Se implementa el formulario y la integración
POST /api/v1/identidad/cuentas; fuentes y pruebas en API_INVENTORY/IMPLEMENTATION.
El OpenAPI público sigue devolviendo cero rutas; no se declara una prueba de creación
real ni de entrega de correo. La activación usa el enlace de correo que emite M7/M10.
