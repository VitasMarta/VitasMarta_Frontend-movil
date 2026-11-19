# Reservas móviles — plan y contrato

## Fuente y alcance

M3 main 49fe4fb9afe38d6d236aad630bafcae55a644b01: inbound/http/app.py y schemas.py.
M1 mantiene /api/v1/reservas y exige sesión. Web es referencia funcional, no diseño.
Se prepara Fase3 en paralelo; root integra y verifica después del catálogo Fase2.

## Plan previo al código y crítica

Sistema existente Poppins, base #FDFBFA, tinta #1D2B5C, verde #006B3F, rosa #F4E1DE;
en oscuro fondo #0D1723/texto #F2F4F7. Listado como agenda vertical con fecha, estado
y acción Abrir; sin tarjetas decorativas ni KPI. Oferta como radio de franjas reales,
importe decimal y política completa antes de la acción explícita Reservar. Detalle
como comprobante legible: referencia, estado real, fechas, cupos, precio, instrucciones
y política. Nunca se anuncia CONFIRMADA para un pendiente.

Crítica: una pantalla no disponible tras login confundía una función pendiente con
un fallo del servidor. Se reemplaza por estado vacío/error/carga/contenido reales.
Sin nombres de destinos inventados: se usa servicio_ref cuando no hay nombre contratado.
La cancelación tiene diálogo nativo, motivo requerido y acción de conservar primero.
Las opciones de pago externo solo muestran instrucciones backend; no se simula pago.

Skills leídas: frontend-design 1.1.1 y modern-web-guidance 1.0.1. Guías dialog/forms
recuperadas completas (638 líneas). 'autofocus on the safe action' se adapta al botón
Conservar de AlertDialog; 'prevent double-posts' a exclusión por ViewModel/repositorio.
MaterialTheme global mantiene claro/oscuro; sin HTML/CSS/WebView ni imágenes ficticias.

## Idempotencia y privacidad

Se emite clave con POST claves-idempotencia y se conserva por intención, cuenta,
payload y correlación. Reintentar tras timeout reutiliza la clave; la UI congela datos
al enviar y solo ofrece cambio explícito después de un rechazo definitivo. Claves
y resultados privados solo en memoria; logout/cambio de generación invalida todo.
No persistir tokens ni exportar comprobantes privados sin acción contextual.
La creación/cancelación real no se ejecuta por automatización contra el despliegue.

## Verificación prevista

MockWebServer comprueba auth, ruta/payload de oferta/reserva, dinero decimal, claves,
respuesta perdida/reintento, conflicto y logout. ViewModel comprueba doble envío,
validación política/cupos/motivo y limpieza por cambio de sesión. La verificación
central de Gradle y dispositivo se ejecutará por root, sin Gradle concurrente.

## Código preparado para integración y verificación central

Se implementaron repositorio/DTO/modelos, listado paginado y filtros, oferta real con
tarifa POR_CUPO/POR_RESERVA y BigDecimal, aceptación explícita, creación, detalle y
cancelación con diálogo nativo. Se amplió la modificación conforme a M3: nuevo
registro con original_ref y cancelación/reemplazo del anterior; misma oferta/servicio.
La respuesta mantiene estados reales y referencias cruzadas, sin anunciar confirmación
para un pendiente.

GET comprobante usa Streaming, valida MIME PDF, firma %PDF- y máximo 10MB; cierra
stream incluso si el guard de generación descarta la respuesta. La ruta entrega
ByteArray a un callback explícito de root para CreateDocument, no guarda en público
ni comparte automáticamente. Los estados permitidos reflejan DescargarComprobante
de M3: SOLICITADA, CONFIRMADA y PENDIENTE_PAGO_EXTERNO. Un documento preliminar
no confirma reserva ni pago.

Se prepararon 18 pruebas JVM: 10 de repositorio/contrato y 8 de ViewModel. Incluyen
reintento explícito tras SocketTimeoutException con clave/correlación estable,
intento duplicado, modificación con referencia sustituta, cancelación, dinero exacto,
PDF inválido/grande, logout, nueva generación del mismo usuario, bloqueo de inputs,
Retry-After sin bypass mediante reset, y entrega de documento solo por acción.
Aún no se ejecutan ni se declaran aprobadas: root ejecuta formato, compilación y
suites en orden después de cerrar Fase2. Las fixtures solo están en src/test.

Firmas: BookingListRoute(onOpen,onExplore); BookingOffersRoute(serviceRef,onBack,
onCreated,existingReference=null); BookingDetailRoute(reference,onBack,onModify,
onDocument). DI RemoteBookingRepository(BookingApi?, AuthenticatedApiExecutor, SessionStore),
como Singleton para conservar claves por intención entre rutas. Shared nav/resources
no fueron editados por este subagente; textos posteriormente integrados en los tres recursos Android por root; propuesta temporal retirada.

Limitación declarada: claves/intenciones no sobreviven a la muerte del proceso;
tras reinicio se debe consultar el historial antes de enviar otra solicitud. No hay
reintentos automáticos de transporte ni mutaciones reales de smoke. La persistencia
segura de una intención incierta queda para una mejora verificada separada.

Guard monetario: longitud <=64, precisión <=24, escala -16..8 y <=16 enteros;
exponentes de hasta tres dígitos y valor absoluto <=32 antes de BigDecimal. Escala
negativa se normaliza a cero después de validar. Ajuste externo permite signo;
tarifas nunca negativas. Hay prueba con 1e100000000 sin expandir el número.

La cantidad también se limita por capacidad_total aun cuando disponibles sea null;
no se interpreta null como cero. Test adicional comprueba cantidad excesiva rechazada
y cantidad positiva dentro de la capacidad enviada al servidor como autoridad final.
