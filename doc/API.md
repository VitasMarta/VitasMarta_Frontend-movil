# Integración API Gateway

Referencia primaria de enrutamiento y seguridad: https://github.com/VitasMarta/servicio_api_gateway (clon local `../servicio_api_gateway`). Su código no pudo verificarse desde acceso público. Referencia de consumo:  https://github.com/VitasMarta/VitasMartaFrontend/blob/main/contratos_de_consumo_api_del_frontend.md . Contrastar contra código del cliente web, OpenAPI y Gateway desplegado. Si hay discrepancia, registrar evidencia y pedir confirmación; no inventar rutas. La URL real de producción no se ha verificado. Configurar API_BASE_URL por variante de build, con HTTP o HTTPS y barra final; no duplicar prefijos. Revisar si la web usa un proxy BFF, cookies o rutas relativas que Android no puede reutilizar literalmente.

## Inventario obligatorio previo a implementar
Crear specs/API_INVENTORY.md con método, ruta, módulo, rol, parámetros, body/response, códigos, headers, fuente (ruta de archivo y commit), estado (documentado/verificado/bloqueado) y prueba. Cubrir todas las funciones del alcance. Las rutas siguientes son puntos de partida documentados, no prueba de disponibilidad en producción.

| Método | Ruta | Uso |
|---|---|---|
| POST | /api/v1/identidad/sesiones | Login |
| POST | /api/v1/identidad/sesiones/refresco | Renovación |
| GET | /api/v1/identidad/usuarios/actual/perfil | Perfil |
| GET | /api/v1/catalogo/categorias | Categorías |
| GET | /api/v1/catalogo/busqueda | Exploración |
| GET | /api/v1/catalogo/{id} | Detalle |
| GET | /api/v1/catalogo/{id}/franjas | Disponibilidad |
| GET | /api/v1/recomendaciones | Sugerencias |

Completar reservas, registro, recuperación, consentimientos, multimedia, alternativas sostenibles y operaciones de prestadores con las fuentes reales; no deducir métodos por nombre.

## Cliente HTTP
Un cliente compartido para Gateway y servicios Retrofit por dominio. Aplicar Accept-Language y X-Correlation-Id conforme al contrato, manteniendo el identificador en reintentos de la misma operación. Bearer únicamente en solicitudes privadas al host autorizado. X-Tenant-Id solo donde el contrato lo exige. Para reservas, obtener clave de idempotencia del servidor cuando corresponda; reutilizar en reintentos de la misma mutación. No repetir automáticamente operaciones no idempotentes. Timeouts finitos, reintentos acotados para lecturas transitorias con backoff y respeto Retry-After. Nunca incluir credenciales en trazas.

## Sesión
Login con formato real del contrato; contemplar MFA cuando el rol lo exija. Access token preferiblemente en memoria; persistir refresh token cifrado si se requiere sesión persistente. Renovación coordinada single-flight para evitar invalidar tokens rotados; actualizar par de tokens atómicamente. Máximo un reintento por solicitud y evitar recursión en refresh/login. Cerrar sesión si refresh es rechazado; invalidar estado y datos del usuario. Distinguir 401 de 403. El backend es autoridad de permisos; los guards de UI no son seguridad efectiva. OAuth solo con flujo nativo seguro confirmado (navegador externo, App Links, PKCE si soportado); no capturar claves en WebView ni suponer compatibilidad de callback web.

## Respuestas
Normalizar envoltura VitasMarta y Problem Details a errores tipados; manejar conectividad, timeout, validación, acceso, bloqueo, conflicto, rate limit y servidor según contrato. Mostrar mensajes seguros traducibles y correlation ID para soporte. Tratar 206 de recomendaciones como resultado degradado válido con aviso, no fallo. Respetar paginación por cursor y atributos opcionales del catálogo. No falsear disponibilidad ni éxito de reserva. Confirmar estado con backend ante respuesta ambigua. No almacenar datos privados en caché compartida.


## Actualización de descubrimiento — 2026-11-17

El Gateway ya pudo inspeccionarse en el commit fijado de [PHASE_0.md](specs/PHASE_0.md).
La advertencia inicial de falta de acceso describe el estado anterior. El inventario
vigente está en [specs/API_INVENTORY.md](specs/API_INVENTORY.md), con fuente, acceso,
payload, estado y prueba pendiente por operación. La URL HTTPS desplegada sigue sin verificar.

M1 no reescribe paths. Android llama M1 directamente, sin el prefijo /bff web.
Alternativas de recomendaciones exigen credenciales en la matriz actual del borde.
No usar las franjas informativas del catálogo para construir una reserva: las ofertas
M3 aportan franja_ref, tarifa, cupos y política vigentes, pendientes de esquema real.
Registro, reset, MFA y forma de M6 presentan divergencias explícitas entre texto y
código web; no se dan por resueltas con suposiciones. Ninguna de las rutas listadas
está probada en un despliegue real durante esta fase.

## Cambio de transporte autorizado — 2026-11-18

Se acepta el Gateway HTTP proporcionado mediante configuración local; no cambian rutas,
payloads ni autenticación. HTTPS sigue disponible con certificados del sistema.
El cliente fija esquema/host/puerto y /api/v1/, no sigue redirecciones ni reutiliza
credenciales para otros hosts. La política Android habilita HTTP solo para el host
configurado. Las observaciones históricas sobre HTTPS no impiden ya conectar la app.
