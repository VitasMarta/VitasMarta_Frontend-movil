# Inventario API móvil — Fase 0

Fecha: 2026-11-17. Alcance provisional: visitante y turista. Inventario estático;
ninguna llamada a un despliegue ni prueba contractual ejecutada.

## Fuentes fijadas

- **G**: [Gateway fb973278](https://github.com/VitasMarta/servicio_api_gateway/tree/fb973278da8b455e9854dc0f76d5546af062fab6).
  **G1** = src/main/resources/application-common.yml (rutas, acceso, matriz 1.2.0).
  **G2** = src/main/java/com/unimag/servicio_api_gateway/infrastructure/web/route/TablaRutasConfig.java.
  **G3** = doc/contratos/CONTRATOS_M1_API_GATEWAY.md.
- **W**: [Web 041dce2a](https://github.com/VitasMarta/VitasMartaFrontend/tree/041dce2a2299262eae23c157f1e3d686e314aad8).
  **W1** = contratos_de_consumo_api_del_frontend.md (sección indicada por módulo).
  **W2** = src/core/api/routes.ts. **W3** = src/core/api/types.ts.
  **W4** = src/features/catalogo/api.ts. **W5** = src/features/reservas/api.ts.
  **W6** = src/features/auth/AuthViews.tsx. **W7** = src/core/auth/session.ts.
  **W8** = src/features/perfil/PerfilView.tsx. **W9** = src/features/perfil/consentimientos.ts.
  **W10** = src/core/auth/preferences.ts. **W11** = src/core/api/idempotency.ts.

Cada fila hereda el commit de su fuente. G1/G2 confirman exposición por prefijo, no
la existencia del handler ni el DTO downstream. **D** = documentado/inspeccionado estáticamente,
pendiente de prueba real. **B** = bloqueado por ambigüedad explícita. Los códigos de éxito
proceden de la fuente indicada; no se deducen códigos de error downstream no publicados.
Todas las pruebas de la última columna están **planificadas, no ejecutadas**.

## Fuente de identidad añadida — 2026-11-18

**I**: [Identidad 95f1d32c](https://github.com/VitasMarta/servicio_identidad/tree/95f1d32c5bebb95ea9fbd864330275b4994e05e1).
**I1** = infrastructure/web/controller/CuentaController.java; **I2** =
infrastructure/web/dto/RegistroRequest.java y ConsentimientoRequest.java;
**I3** = domain/model/PoliticaPassword.java y application/usecase/cuenta/RegistrarUsuarioUseCase.java;
**I4** = src/main/resources/application.yml y ObjectMapperJackson2Config.java (snake_case).
Rutas Java bajo src/main/java/com/unimag/servicio_identidad/. Copia local de lectura
bajo .git/vitasmarta-references/; no se modifica el servicio. Esta fuente resuelve
D03; el Gateway conserva autoridad para enrutamiento/acceso. OpenAPI remoto sigue vacío.

## Cabeceras y errores comunes

**H**: Accept: application/json, Accept-Language: es-CO/en-US, X-Correlation-Id: UUID v4 por
intención (conservar en reintento), Content-Type: application/json si hay cuerpo JSON.
Preferencia explícita de idioma: X-User-Locale; observar X-Resolved-Locale de respuesta.
**A**: H + Authorization: Bearer del usuario en rutas privadas del host M1 autorizado.
**K**: A + Idempotency-Key emitida por M3. **PDF**: A + Accept: application/pdf.
Sin X-Tenant-Id en turista; no copiar X-API-Key ni secretos de servicio en el APK.
El cliente de imágenes CDN no recibe Bearer, cookies de sesión ni cabeceras privadas.

Errores M1 comprobados en CodigoErrorBorde.java: 400 SOLICITUD_INVALIDA,
401 TOKEN_INVALIDO, 403 ACCESO_DENEGADO, 404 RUTA_NO_ENCONTRADA, 429 LIMITE_TASA,
503 SERVICIO_NO_DISPONIBLE. Envoltura: codigo, mensaje, marca_temporal, correlation_id,
detalles opcionales. Problem Details de M4 se normalizará cuando se aborde ese rol.
Respetar Retry-After si está presente; no reintentar mutaciones ciegamente.

En el borde, rutas privadas sin regla específica usan AUTENTICADA; los roles de las tablas
siguientes son los del contrato funcional downstream (W1). No afirmar que M1 los restringe
por sí solo. El backend conserva la autoridad sobre permisos y titularidad.

## Identidad M7

| ID | Método y ruta expuesta M1 | Rol / headers | Parámetros y cuerpo | Respuesta / códigos documentados | Fuente | Estado | Prueba prevista |
|---|---|---|---|---|---|---|---|
| AUTH-01 | POST /api/v1/identidad/sesiones | Público / H | {correo,password} | 200 {access_token,refresh_token,expira_en} o desafío MFA; 401/423 | G1; W1 §2.1B; W7 | D | Login válido, inválido, bloqueo; no persistir password |
| AUTH-02 | POST /api/v1/identidad/sesiones/refresco | Público / H | {refresh_token} | 200 nuevo par rotado; 401 termina sesión | G1; W1 §2.1B; web src/app/bff/api/v1/identidad/sesiones/refresco/route.ts | D | 401 concurrentes generan un refresh; rotación atómica; máximo un reintento; logout durante refresh |
| AUTH-03 | DELETE /api/v1/identidad/sesiones | Titular / A | Sin body documentado | 200 revoca sesión | G1; W1 §2.1B; W7 | D | Limpieza local aun sin red; no restaurar token de petición tardía |
| AUTH-04 | POST /api/v1/identidad/cuentas | Público / H | {correo,password,tipo_usuario:TURISTA,consentimientos:[{finalidad,otorgado}]} | 201 {mensaje}; no crea sesión; cuenta pendiente de verificación | G1; I1–I4; W6 | D03 resuelta por fuente M7; implementado y probado localmente | Ruta/payload/201/errores; 12–128 caracteres; opcionales independientes; doble toque; sin registro real automático |
| AUTH-05 | POST /api/v1/identidad/cuentas/verificacion | Público / H | {token} | 200 activa cuenta | G1; W1 §2.1A; W6 | D | Token válido/expirado; no registrar ni persistir enlace sensible |
| AUTH-06 | POST /api/v1/identidad/cuentas/reenvio-verificacion | Pública en M1; W1 exige Bearer / por confirmar | Body no especificado | 200 | G1; W1 §2.1A | B | Confirmar cómo identificar cuenta pendiente sin sesión; no deducir body |
| AUTH-07 | POST /api/v1/identidad/password/recuperacion | Público / H | {correo} | 202 siempre: respuesta anti-enumeración | G1; W1 §2.1A; W6 | D | Mismo mensaje para correo existente/no existente |
| AUTH-08 | POST /api/v1/identidad/password/restablecimiento | Público / H | W1 {token,nueva_password}; W6 {token,password} | 200 invalida sesiones | G1; W1 §2.1A; W6 | B D04 | Contrato confirmado; token expirado; contraseñas coincidentes |
| AUTH-09 | POST /api/v1/identidad/sesiones/mfa | Público / H + contexto | W1 codigo_totp; W7 {mfa_token,codigo} | 200 par de tokens | G1; W1 §2.1B; W7 | B D05 | Desafío real, expiración, código inválido; nunca tratar mfa_token como access |
| AUTH-10 | GET /api/v1/identidad/oauth/{proveedor}/inicio | Público | proveedor google/facebook; callback nativo sin confirmar | 302 | G1; W1 §2.1A; W2 | B | Confirmar navegador externo, PKCE/callback y App Links antes de habilitar |
| AUTH-11 | GET /api/v1/identidad/oauth/{proveedor}/callback | Público | code,state; reglas nativas pendientes | 200 tokens según W1 | G1; W1 §2.1A | B | No copiar callback web a Compose; sin URI inventada |

## Catálogo M2 y contenido ambiental M8

| ID | Método y ruta expuesta M1 | Rol / headers | Parámetros y cuerpo | Respuesta / códigos documentados | Fuente | Estado | Prueba prevista |
|---|---|---|---|---|---|---|---|
| CAT-01 | GET /api/v1/catalogo/categorias | Visitante / H sin Bearer | — | 200 [{slug,nombre:{locale:texto},primer_nivel}] | G1; W1 §2.2; W3/W4 | D | Bilingüe, vacío, error; sin categorías inventadas |
| CAT-02 | GET /api/v1/catalogo/busqueda | Visitante / H | texto,categorias repetidas,lat,lng,radio_km,precio_minimo,precio_maximo,atributos JSON,orden,descendente,limite 1..100,cursor | 200 {items,total,cursor_siguiente,desde_cache,frescura_ultima_actualizacion}; ítem M2: id,nombre,categorias,precio_minimo/precio_maximo,imagen_url | G1; W1 §2.2; W3/W4 | Confirmado M2; implementado | Cancelación/debounce; cursor opaco; filtros conservados; caché indicada; red/esquema inválido |
| CAT-03 | GET /api/v1/catalogo/{id} | Visitante / H | id codificado; idioma opcional según W1 | 200 ficha M2: id,nombre,descripcion,categorias,ubicacion,atributos,multimedia,rango_precio,franjas,frescura; opcionales respetados | G1; W1 §2.2; W3/W4 | Confirmado M2; implementado | Solo campos presentes; no sustituir null por cero; 404, traducción |
| CAT-04 | GET /api/v1/catalogo/{id}/franjas | Visitante / H | id | 200 {id,franjas:[{fecha,hora_inicio,hora_fin,cupo,estado}]} | G1; W1 §2.2; W3/W4 | D | Vacío; fechas locales; no usar como autorización para reservar |
| CAT-05 | GET /api/v1/catalogo/{id}/multimedia | Visitante / H | id | 200 [{tipo,url_cdn,tam_bytes,orden}] | G1; W1 §2.2; W3/W4 | D | Orden; HTTPS CDN; carga/error; no enviar credenciales; captions/accesibilidad a confirmar para video |
| ENV-01 | GET /api/v1/ambiental/atractivos/{id}/buenas-practicas | Visitante / H | id | 200 {atractivo_ref,contenido,fecha_publicacion}; 204 omite sección | G1; W1 §2.6; W4 | D | 204 sin parsear JSON; 503 aislado del detalle; no ocultar errores arbitrarios |

`orden` documentado: relevancia, nombre, distancia, precio, popularidad, sostenibilidad.
Coordenadas solo con consentimiento contextual o búsqueda manual. No se infiere relación
entre id de catálogo y servicio_ref vendible hasta confirmar esquema real de las ofertas.

## Reservas M3

| ID | Método y ruta expuesta M1 | Rol / headers | Parámetros y cuerpo | Respuesta / códigos documentados | Fuente | Estado | Prueba prevista |
|---|---|---|---|---|---|---|---|
| BOOK-01 | GET /api/v1/reservas/ofertas/{servicio_ref} | Turista / A | servicio_ref real | 200 {servicio_ref,ofertas:[{servicio_ref,franja_ref,inicio,fin,importe,moneda,disponibles,politica,reservable,...}]} | G1 prefijo; W2/W3/W5 | Confirmado M3; implementado | Confirmar esquema OpenAPI, relación a catálogo y tarifa; cupos null ≠ cero; política vigente |
| BOOK-02 | POST /api/v1/reservas/claves-idempotencia | Turista/prestador / A | {operacion:iniciar/cancelar/modificar/confirmar/rechazar,reserva_ref:null o ref} | 201 {clave_idempotencia,operacion} | G1; W1 §2.4; W11 | D | Clave del servidor; no duplicar al reintentar misma intención |
| BOOK-03 | POST /api/v1/reservas | Turista / K | {servicio_ref,franja_ref,cantidad_cupos,aceptacion_politica:{aceptada:true,politica_ref,version}} | 201 ReservaRespuesta; 409 RESERVA_RESTRINGIDA documentado | G1; W1 §2.4; W3/W5 | Confirmado M3; implementado | Doble toque; conflicto; respuesta perdida; reutilizar K y correlación; no asumir confirmada |
| BOOK-04 | GET /api/v1/reservas | Titular turista/prestador / A | page=0,size=20,clasificacion=ACTIVAS/PASADAS/CANCELADAS | 200 {content,total_elements,page,size} | G1; W1 §2.4; W3/W5 | D | Solo reservas del titular; vacío; paginación; 401/403 distintos |
| BOOK-05 | GET /api/v1/reservas/{ref} | Titular / A | ref | 200 ReservaRespuesta | G1; W1 §2.4; W3/W5 | D | Referencia ajena denegada; estado actualizado; expiración desde vence_en |
| BOOK-06 | POST /api/v1/reservas/{ref}/cancelar | Titular turista/prestador / K | {motivo} | 200 ReservaRespuesta con penalidad_aplicada_porcentaje | G1; W1 §2.4; W5 | D | Confirmación explícita, penalidad real, reintento con misma intención |
| BOOK-07 | POST /api/v1/reservas/{ref}/modificar | Turista / K | CrearReservaEntrada como BOOK-03 | 200 con original_ref/reemplazo_ref | G1; W1 §2.4; W5 | Confirmado M3; implementado | Oferta y política reales; no perder referencia original en conflicto |
| BOOK-08 | GET /api/v1/reservas/{ref}/comprobante | Titular / PDF | ref | 200 PDF binario | G1; W1 §2.4; W5 | D | Guardado/compartir contextual; no exponer token en URL ni almacenamiento público automático |

ReservaRespuesta documentada: reserva_ref, codigo_confirmacion?, estado, fecha_inicio?,
fecha_fin?, vence_en?, tarifa?, politica?, instrucciones_pago_externo?, siguiente_accion?,
original_ref?, reemplazo_ref?. Importes se conservan como decimal exacto, con moneda del
contrato. Nada de dinero en Float/Double ni de asumir COP por ausencia del campo.
Instantes de oferta M3 son UTC según W3; presentación en America/Bogota. No hardcodear
10 minutos: vencimiento y siguiente acción provienen del servidor. Mutación ambigua:
conservar clave/intención y resolver con M3, sin mostrar éxito ni crear otra reserva.

## Perfil, preferencias y privacidad M7

| ID | Método y ruta expuesta M1 | Rol / headers | Parámetros y cuerpo | Respuesta / códigos documentados | Fuente | Estado | Prueba prevista |
|---|---|---|---|---|---|---|---|
| PRO-01 | GET /api/v1/identidad/usuarios/actual/perfil | Titular / A | — | 200 datos titular | G1; W1 §2.1C; W8 | D | Cambio de cuenta/logout cancela y limpia datos |
| PRO-02 | PUT /api/v1/identidad/usuarios/actual/perfil | Titular / A | M7 {nombre_visible}; no sobrescribir datos_contacto | 200 | G1; W1 §2.1C; W8 | Confirmado M7; implementado | Solo campos autorizados; validación backend; no reenviar campos privados/read-only |
| PRO-03 | GET /api/v1/identidad/usuarios/actual/preferencias | Titular / A | — | 200 {idioma_preferido,canal_autorizado?,idioma_autorizado?} | G1; W1 §2.1C; W10 | D | Preferencia servidor; fallo de lectura no sobrescribe otros ajustes |
| PRO-04 | PUT /api/v1/identidad/usuarios/actual/preferencias | Titular / A | M7 campos string; el cliente envía solo {idioma_preferido} | 200 | G1; W1 §2.1C; W10 | B | Tipos confirmados en M7; conservar otros campos; actualizar idioma nativo tras éxito |
| PRO-05 | GET /api/v1/identidad/usuarios/actual/intereses | No consumir | — | Sin handler en M7 auditado | M7; W8 divergente | No implementado en servicio | No inventar lectura; reemplazo explícito mediante PRO-06 |
| PRO-06 | PUT /api/v1/identidad/usuarios/actual/intereses | Turista / A | {intereses:string[]} reemplazo atómico | 200 | G1; W1 §2.1C; W8 | D | Lista vacía; rechazo; no anunciar guardado antes de respuesta |
| PRO-07 | GET /api/v1/identidad/usuarios/actual/consentimientos | Titular / A | — | W9 {vigente:{FINALIDAD:{otorgado,version_politica,fecha_hora}},historial}; W1 no fija forma | G1; W1 §2.1C; W9 | B | Confirmar objeto vigente/historial; ausente ≠ permiso concedido |
| PRO-08 | PUT /api/v1/identidad/usuarios/actual/consentimientos/{finalidad} | Titular / A | finalidad TRATAMIENTO/COMERCIAL/PERFILADO; {otorgado:boolean} | 200 | G1; W1 §2.1C; W8 | D | Opcionales independientes; no habilitar revocación TRATAMIENTO sin confirmar consecuencias |
| PRO-09 | POST /api/v1/identidad/usuarios/actual/solicitudes-arco | Titular / A | {tipo:ACCESO/RECTIFICACION/CANCELACION/OPOSICION,detalle:objeto} | 201 {solicitud_ref,...} | G1; W1 §2.1C; W3/W8 | D | Confirmación explícita; seguimiento, sin prometer borrado inmediato |
| PRO-10 | GET /api/v1/identidad/usuarios/actual/solicitudes-arco | Titular / A | — | 200 solicitudes (forma W8 array, pendiente OpenAPI) | G1; W1 §2.1C; W8 | D | Lista vacía, error, pertenencia |
| PRO-11 | GET /api/v1/identidad/usuarios/actual/solicitudes-arco/{id} | Titular / A | solicitud id | 200 {solicitud_ref,tipo,detalle,estado,pasos,...} según W3 | G1; W1 §2.1C; W2/W3 | D | Pasos reales, sin suponer siempre cinco completados |
| PRO-12 | GET /api/v1/identidad/usuarios/actual/exportacion-datos | Titular / PDF | — | 200 PDF | G1; W1 §2.1C; W8 | D | Binario privado; error y cancelación; no persistir credenciales |

## Recomendaciones M6

| ID | Método y ruta expuesta M1 | Rol / headers | Parámetros y cuerpo | Respuesta / códigos documentados | Fuente | Estado | Prueba prevista |
|---|---|---|---|---|---|---|---|
| REC-01 | GET /api/v1/recomendaciones | Turista según servicio / A | limite default 5 máx 10; categoria?,lat?,lng? | 200 o 206 éxito degradado; M6 recomendaciones[], metadatos_ia, modo, advertencias | G1; W1 §2.3; W3/W4 | Confirmado M6; implementado | Esquema real; opt-out; 206 mantiene contenido; sin convertir métricas desconocidas en cero |
| REC-02 | GET /api/v1/recomendaciones/alternativas/{id} | Autenticada en M1; turista funcional / A | atractivo id | 200 alternativas y mensaje ambiental según W1 | G1; W1 §2.3; W4 | Sin handler M6; no consumir | Abrir IDs de alternativas_sugeridas en la ficha M2 |

## Roles posteriores y rutas excluidas por ahora

No equivalen a exclusión definitiva: falta confirmar alcance. No iniciar implementación.

| Rol | Funciones observadas | Referencia y limitación |
|---|---|---|
| Prestador | Onboarding, oferta, franjas, políticas, reservas recibidas, check-in, indicadores | W1 §2.5; W2; requieren inventario M4 propio y resolver D08 antes de implementar |
| Administrador | Usuarios, matriz, auditoría, moderación, capacidades, estado de servicios | G1; W1 §2.1D/2.5D/2.6; MFA y alcance explícito |
| Analista turístico | Reportes y exportaciones | G1; W1 §2.7; sin aprobación para entrega móvil |
| Autoridad ambiental | Sostenibilidad agregada y lectura de configuración | G1; W1 §2.6/2.7; sin operaciones comerciales |
| Integrador técnico | Integración y acceso técnico | G1; requiere MFA según contrato, sin secretos en APK |

`/api/v1/**/internal/**`, `/api/v1/identidad/interno/**` y
`/api/v1/ambiental/atractivos/*/estado-sostenibilidad` están denegados a humanos en G1.
No implementar notificaciones administrativas ni telemetría de infraestructura para turistas.
El inventario queda pendiente de ratificación por `/api-docs` y pruebas autorizadas;
los DTO ambiguos permanecen bloqueados expresamente.


## Evidencia de implementación local (no ratificación de despliegue)

| Operaciones | Implementación y prueba | Estado remoto |
|---|---|---|
| AUTH-01/02/03 | IdentityApi, RemoteAuthRepository, SessionStore; tests de payload, rechazo, MFA sin sesión, single-flight, logout concurrente y reintento único | Sin prueba privada real; HTTP autorizado, faltan credenciales de prueba |
| AUTH-07 | AccountRecoveryRepository/RecoveryViewModel; correo único, 202 vacío, doble envío y confirmación anti-enumeración | Sin prueba real |
| CAT-01 | CatalogApi/RemoteCatalogRepository/CatalogViewModel; traducciones, error de esquema y recuperación de red | Sin prueba real |

Las fixtures y servidores falsos residen exclusivamente en src/test. Los DTO ambiguos
siguen bloqueados; estas pruebas no convierten un esquema inferido en contrato confirmado.

## Registro implementado

El formulario móvil usa RegistrationApi/RemoteRegistrationRepository y RegistrationViewModel.
No envía aceptado, version_terminos, prestador_ref ni confirmación de contraseña.
La versión de política la decide M7. Solo se muestra cuenta creada con 201 y mensaje
no vacío; se pide verificar correo antes de iniciar sesión y no se instala una sesión.
MockWebServer comprueba payload, ausencia de Bearer, rechazo de respuestas malformadas
y errores 400/409/429/503. No se crean cuentas en el despliegue para probar.

## Auditoría actual de catálogo, reservas y recomendaciones — 2026-11-18

Fuentes de solo lectura y hashes completos en [REPOSITORY_AUDIT_2026-11-18.md](REPOSITORY_AUDIT_2026-11-18.md):
M2 c71ad902, M3 49fe4fb9 y M6 904673be. El DTO de búsqueda/ficha está definido en
M2 application/dto/consulta.py y ficha.py; ofertas/reservas en M3 inbound/http/schemas.py;
recomendaciones en M6 app/schemas.py. La falta de OpenAPI agregado no bloquea consultar
estos contratos de código. Los consumidores móviles están implementados y tienen pruebas JVM; evidencia vigente al final.

Correcciones vigentes a las filas históricas:
- CAT-01 se verificó anónimamente: 200, seis categorías.
- CAT-02 respondió 200 con items=[], total=0. DTO de ítem y cursor inspeccionados; no se obtuvo ítem real del despliegue.
- CAT-03 tiene handler y DTO M2 comprobados por fuente; sin ID real visible no se hizo smoke de ficha.
- REC-01: M6 usa recomendaciones[], no items[]; 200/206 están definidos por modo. Se probó solo 401 anónimo.
- REC-02: M6 main no tiene handler /alternativas/{id}, aunque la web lo invoca; no tratarlo como endpoint implementado. Alternativas_sugeridas se incluyen en cada ítem de M6.
- La lista privada real de reservas se obtiene por GET /api/v1/reservas; la auditoría solo comprobó 401 anónimo.

La observación original de pantallas sin consumidores quedó resuelta: ServiceMessage se retiró; reservas y recomendaciones consultan sus contratos reales.

## Consumidores vigentes y límites de verificación — 2026-11-18

Fuentes adicionales de perfil: M7 95f1d32c, PerfilController/ArcoController y DTO.
Las tablas de Fase 0 conservan referencias web como antecedente; las siguientes
precisiones prevalecen sobre campos/códigos pendientes de esa propuesta.

| Grupo | Consumidor y validación local | Verificación remota |
|---|---|---|
| CAT-01/02/03/04 | Categorías, búsqueda/filtros/cursor, ficha/franjas; esquema inválido, dinero exacto, paginación y cancelación probados | Categorías y búsqueda real de categoría por UI; sin atractivo real para ficha |
| CAT-05 | Multimedia de ficha M2; carga de imágenes HTTPS sin Bearer mediante cliente independiente | Sin archivo real disponible en catálogo auditado |
| BOOK-01..08 | Ofertas, clave del servidor, crear/listar/detalle/cancelar/modificar/PDF; tests de cuerpo, política, idempotencia, Retry-After y sesión | No se efectuaron mutaciones ni lecturas privadas con credenciales reales |
| PRO-01..04/06..12 | Perfil, idioma, intereses PUT, consentimientos, ARCO y PDF; repositorio/ViewModel probados | Sin cambios reales sobre la cuenta del propietario |
| PRO-05 | Sin consumidor; no hay GET M7 | No se inventa endpoint |
| REC-01 | recomendaciones[], 200/206, metadatos y alternativas embebidas; pruebas de repositorio/ViewModel | Auditoría solo 401 anónimo; fixture de UI no es resultado personalizado real |
| REC-02 | Sin consumidor; sin handler M6 | No se llama desde la app |

Consentimientos lee vigente y modifica únicamente COMERCIAL/PERFILADO; ausencia
no equivale a permiso. Preferencias contiene strings idioma_preferido, idioma_autorizado
y canal_autorizado; actualizar idioma envía únicamente idioma_preferido. ARCO
lista es array; detalle tiene pasos, marca_temporal y resultado. Solicitud de
cancelación se confirma expresamente y limpia sesión solo después del 201.

Datos ambientales independientes ENV-01 y vídeo no tienen consumidor específico
en este corte; la ficha presenta atributos/multimedia soportados y M6 conserva
estado ambiental desconocido sin convertirlo en cero. No declarar ese contenido
como verificado por tener pruebas de catálogo.

104 pruebas JVM/MockWebServer aprobadas. Cargas públicas físicas aprobadas;
credenciales y operaciones privadas reales no fueron extraídas ni automatizadas.

## Relectura del despliegue tras el aviso del propietario

GET búsqueda con los parámetros reales del móvil devuelve ahora tres registros;
GET filtro gastronomico devuelve uno. Las tres fichas responden 200 con idioma
explícito, y /franjas responde 200 con lista vacía. La lectura anterior total=0
era una observación de otro momento. No concluye que falten eventos publicados.
M3 requiere sesión: no se infiere reservabilidad de la ausencia de franjas M2.
Referencia: [REPOSITORY_RECHECK_2026-11-18.md](REPOSITORY_RECHECK_2026-11-18.md).
