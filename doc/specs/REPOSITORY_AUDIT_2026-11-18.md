# Auditoría del estado actual de VitasMarta

Fecha de consulta: 2026-11-18T09:27:02-05:00.

## Aviso de vigencia

Este informe refleja el corte inicial de las 09:27. La implementación móvil ya
incorpora los flujos descritos como pendientes y el Gateway devuelve tres contenidos
en la revisión posterior. Consultar [revisión vigente](REPOSITORY_RECHECK_2026-11-18.md)
y [pruebas actuales](IMPLEMENTATION.md). No usar total=0 como estado actual.

## Conclusión histórica

La causa principal de lo observado es una implementación móvil parcial. Reservas y Para ti muestran un mensaje fijo sin llamar al backend; las categorías carecen de acción de navegación. No es evidencia de caída de M3/M6 ni de fallo de la cuenta. La entrega móvil no cumple todavía el MVP definido.

Además, la búsqueda pública del Gateway devuelve cero atractivos. Las categorías son una taxonomía: seis categorías no equivalen a seis destinos. Aun completando la UI, actualmente la búsqueda debe mostrar un vacío real. No se puede atribuir ese vacío a una causa concreta de infraestructura sin registros o acceso administrativo.

## Alcance de la revisión

- Se consultaron los 12 repositorios accesibles del propietario VitasMarta, sus ramas main actuales, PR abiertos y últimos runs de GitHub Actions filtrados por main.
- Copias nuevas bajo .git/vitasmarta-references/audit-2026-11-18 preservan las referencias anteriores. Las fuentes de los otros repositorios no se modificaron; solo se actualiza documentación móvil.
- Se inspeccionaron handlers, DTO y consumo web de catálogo/reservas/recomendaciones; rutas y capacidades de los demás servicios. Esto es una auditoría funcional de integración, no una certificación exhaustiva de todos los módulos.
- Solo lecturas HTTP sin autenticación. No se extrajeron tokens del teléfono, no se usó la cuenta del propietario y no se crearon reservas, registros ni mutaciones.
- No se recompilaron ni ejecutaron suites de backend/web. No se puede afirmar que el commit desplegado sea el mismo que main sin evidencia del despliegue.

## Estado por repositorio

| Repositorio / main inspeccionado | Último commit (fecha del commit) | Estado observado |
|---|---|---|
| [VitasMarta_Frontend-movil](https://github.com/VitasMarta/VitasMarta_Frontend-movil/tree/d49342fb9bfcfb0beb831966e513e787d1b44dbf) | `d49342fb` · 2026-11-17T21:17:37-05:00 | main remoto solo contiene README. El código Android está local, sin commit/push. Login/registro/categorías/tema existen; búsqueda/ficha/reservas/recomendaciones/perfil completo faltan. |
| [VitasMartaFrontend](https://github.com/VitasMarta/VitasMartaFrontend/tree/041dce2a2299262eae23c157f1e3d686e314aad8) | `041dce2a` · 2026-11-17T20:17:50-05:00 | Tiene búsqueda, ficha, panel de ofertas y creación/historial/detalle/cancelación de reservas; consume M6 con un adaptador. Es referencia funcional, no prueba de ejecución remota completa. |
| [servicio_api_gateway](https://github.com/VitasMarta/servicio_api_gateway/tree/fb973278da8b455e9854dc0f76d5546af062fab6) | `fb973278` · 2026-11-16T10:39:13-05:00 | M1 tiene rutas a los nueve servicios y reglas de acceso. No reescribe prefijos. El despliegue responde catálogo y rechaza accesos privados sin token; el OpenAPI agregado está vacío. |
| [servicio_catalogo](https://github.com/VitasMarta/servicio_catalogo/tree/c71ad9027af5683aefe31c7b0616d84bbafba6c2) | `c71ad902` · 2026-11-17T17:21:28-05:00 | M2 tiene búsqueda paginada, DTO de ítem, ficha, multimedia y franjas, montados también bajo /api/v1. Esos contratos pueden implementarse en móvil. La búsqueda pública desplegada devolvió total=0. |
| [servicio_reservas](https://github.com/VitasMarta/servicio_reservas/tree/49fe4fb9afe38d6d236aad630bafcae55a644b01) | `49fe4fb9` · 2026-11-15T23:17:03-05:00 | M3 tiene ofertas reservables, claves idempotentes, creación, historial, detalle, comprobante y acciones. App móvil no tiene cliente ni UI de negocio para estas rutas. Runtime privado no verificado. |
| [servicio_prestadores](https://github.com/VitasMarta/servicio_prestadores/tree/8bfdf6ebc0b1e2f6e8effafb59bc6e654b44ad7c) | `8bfdf6eb` · 2026-11-17T17:21:31-05:00 | M4 implementa oferta de prestadores, atractivos/actividades/eventos, revisión editorial, multimedia, disponibilidad y outbox. Es fuente de contenido/oferta que se proyecta en M2/M3; no verificado en despliegue privado. |
| [servicio_integracion](https://github.com/VitasMarta/servicio_integracion/tree/3e7129e2b6f29145d3e6ec6d129f80bc589657f2) | `3e7129e2` · 2026-11-16T16:37:31-05:00 | M5 tiene ingesta/unificación, fuentes, webhooks, conflictos, fallidos/reprocesamiento y workers. Puede aportar contenido al catálogo; no se verificaron sincronizaciones ni datos operativos remotos. |
| [servicio_recomentaciones_IA](https://github.com/VitasMarta/servicio_recomentaciones_IA/tree/904673bee7f0999801d45fc2784a6b43471c11ec) | `904673be` · 2026-11-17T17:21:33-05:00 | M6 implementa GET /api/v1/recomendaciones, respuesta recomendaciones[], metadatos y fallback 206. No existe handler /alternativas/{id} en este main; sí alternativas_sugeridas dentro de los ítems. App no lo consume. |
| [servicio_identidad](https://github.com/VitasMarta/servicio_identidad/tree/95f1d32c5bebb95ea9fbd864330275b4994e05e1) | `95f1d32c` · 2026-11-17T17:25:32-05:00 | M7 implementa registro/sesión/refresh, verificación, recuperación, perfil, preferencias, intereses, consentimientos y ARCO. Android solo integra parte. El propietario confirmó login real; no se reutilizó su sesión en esta auditoría. |
| [servicio_ambiental](https://github.com/VitasMarta/servicio_ambiental/tree/3cb14d6b630f5c6cad4a06d129eb955a6817cbd5) | `3cb14d6b` · 2026-11-17T17:21:35-05:00 | M8 tiene buenas prácticas, sostenibilidad/ocupación, capacidad, alertas y restricciones de reserva. Hay rutas internas y administrativas; el móvil aún no integra el bloque público de buenas prácticas. |
| [servicio_reportes](https://github.com/VitasMarta/servicio_reportes/tree/b05d3b963b86840eb38257a95531b1b92169eb21) | `b05d3b96` · 2026-11-17T17:39:26-05:00 | M9 implementa indicadores, dashboards, sostenibilidad, pipeline y exportaciones. Principalmente analistas/administración/prestadores, no es la causa del turista bloqueado en la app. |
| [servicio_notificaciones](https://github.com/VitasMarta/servicio_notificaciones/tree/c20c38220929e3271853dc60d55acbdbcf5f072d) | `c20c3822` · 2026-11-17T18:30:03-05:00 | M10 tiene consumidores de eventos, plantillas, envío de correos y administración de envíos. La auditoría no verificó entrega de correo ni estado del proveedor externo. |

## Prueba directa de los síntomas móviles

- app/src/main/java/co/vitasmarta/mobile/navigation/VitasMartaApp.kt:154–164: Reservations y Recommendations invocan AccountRoute y luego ServiceMessage(). No hay llamada de negocio de esas features.
- core/designsystem/ServiceMessage.kt:18: texto estático de servicio no disponible. Es una pantalla provisional, no un resultado HTTP.
- feature/catalog/presentation/CatalogScreen.kt:205: la categoría se dibuja con Row/Text/HorizontalDivider; no tiene clickable ni callback de abrir categoría.
- feature/catalog/data/CatalogApi.kt:15: solo define GET categorías. No existe todavía búsqueda remota ni ficha en el cliente móvil.
- feature/auth/presentation/AuthScreen.kt:65–89: el estado autenticado solo muestra sesión activa y cerrar sesión, además del contenido que aporta la ruta. No carga el perfil remoto.
- La búsqueda visible de categorías filtra datos locales; no busca atractivos ni sustituye GET /catalogo/busqueda.

## Respuestas actuales del Gateway

La URL queda en local.properties ignorado, no en este informe. Consultas anónimas:

| Lectura | Resultado observado | Interpretación válida |
|---|---|---|
| GET /api/v1/catalogo/categorias | 200, seis entradas, claves slug/nombre/primer_nivel | Catálogo responde a esta operación pública. |
| GET /api/v1/catalogo/busqueda?limite=1 | 200, items=[], total=0, cursor_siguiente=null | No se devolvió ningún atractivo visible para esta búsqueda sin filtros. |
| GET /api/v1/recomendaciones | 401 TOKEN_INVALIDO | Comportamiento esperado sin sesión. No demuestra que M6 esté caído o funcionando correctamente con sesión. |
| GET /api/v1/reservas | 401 TOKEN_INVALIDO | Ruta de historial confirmada en M3; no se probó con credenciales. |
| GET /api-docs | 200, paths/components vacíos, nueve servicios declarados no disponibles | Fallo/incompatibilidad de agregación de documentación; no equivale a nueve servicios de negocio caídos. |

También se observó 401 anónimo en /api/v1/reservas/itinerarios, pero esa lectura no se usa como prueba de que exista un handler M3: el borde puede rechazar autenticación antes del enrutamiento.

## Contratos ahora comprobables y diferencias

1. M2 c71ad902: src/catalogo/application/dto/consulta.py define ItemResultadoBusqueda y ResultadoBusqueda. dto/ficha.py define FichaDetalleDTO con campos opcionales y multimedia. presentation/api/main.py monta /v1 y /api/v1. D09 ya no justifica esperar al OpenAPI para diseñar los DTO móviles.
2. M3 49fe4fb9: src/reservas/infrastructure/adapters/inbound/http/app.py y schemas.py fijan GET/POST /api/v1/reservas, ofertas/{servicio_ref}, claves-idempotencia y las acciones. Una reserva necesita la oferta/franja/tarifa/política de M3, no solo el slug de categoría ni las franjas informativas de M2.
3. M6 904673be: app/api/routes.py y app/schemas.py fijan GET recomendaciones, recomendaciones[], metadatos_ia, modo y 200/206. El consumo básico se puede implementar sin inventar una respuesta items[]. D06 se acota al adaptar el DTO real.
4. En M6 main no existe /api/v1/recomendaciones/alternativas/{id}; el frontend web sí lo invoca en src/features/catalogo/api.ts. No debe copiarse como endpoint confirmado. M6 expone alternativas_sugeridas dentro de cada recomendación.
5. M2 mantiene un read model alimentado por M4/M5 vía eventos; M4 publica oferta por outbox y RabbitMQ. Publicaciones pendientes, ausencia de datos o proyección sin aplicar son hipótesis de diagnóstico del catálogo vacío, no causas comprobadas.

## Incompatibilidad comprobada de la documentación OpenAPI

En M1, OpenApiAggregator.java:125–135 construye la URL de todos los servicios
concatenando /v3/api-docs. M3 publica su especificación en /openapi.json
(inbound/http/app.py:124); M7 desactiva springdoc.api-docs por defecto y en prod
(application.yml:87–90). Estas configuraciones de código son incompatibles con
la suposición uniforme del agregador. Explican por qué no debe inferirse caída
de negocio desde x-servicios-no-disponibles. No se verificaron las configuraciones
efectivas de cada contenedor; no se afirma que este sea el único motivo remoto.

## Git, CI y pruebas

- No había PR abiertos en ninguno de los 12 repositorios al consultar.
- El remoto móvil main sigue en Initial commit (solo README). El proyecto Android completo y sus cambios están en el árbol de trabajo local, sin commit/push, conforme a las reglas de autorización.
- No se encontraron runs de GitHub Actions de main en diez repositorios. M9 muestra un success de un SHA anterior; M3 muestra un run Graph Update de dependencias, también anterior. Esos resultados no certifican los commits main actuales ni los despliegues.
- Las 44 JVM y cinco instrumentadas previamente aprobadas del móvil cubren autenticación/registro/base/categorías/tema. No cubren funcionalidades de reservas, ficha o recomendaciones que aún no existen. El número de tests no demuestra que el MVP esté completo.

## Orden de cierre propuesto

1. Completar Fase 2: tocar categoría → búsqueda real con filtro → lista paginada → ficha real, multimedia y vacío/error. Añadir pruebas de esas operaciones con los DTO M2 confirmados.
2. Revisar el contenido desplegado de M2 con el equipo responsable de M4/M5/proyecciones. El móvil debe reflejar un vacío real mientras no haya atractivos publicados; no inventar destinos.
3. Fase 3: listado y detalle de reservas del titular, oferta/franjas M3, idempotencia y creación/cancelación según contrato. Probar lecturas con cuenta autorizada y solicitar autorización concreta antes de generar reservas reales.
4. Fase 4: perfil/intereses/consentimientos y recomendaciones M6 con fallback 206. Resolver aparte cualquier funcionalidad de alternativas sin handler confirmado.
5. Sustituir el mensaje genérico provisional por estados que distingan funcionalidad pendiente, lista vacía, error real y falta de permisos. No presentar una integración pendiente como caída del servidor.

No se modificó código de la app ni del backend durante esta auditoría; este informe explica el estado actual y corrige el diagnóstico de los bloqueos.
