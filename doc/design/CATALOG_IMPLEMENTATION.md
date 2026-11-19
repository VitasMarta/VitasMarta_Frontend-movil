# Catálogo nativo — plan antes del código

## Fuentes confirmadas

M2 c71ad902: application/dto/consulta.py y ficha.py, routers búsqueda/ficha y
presentation/api/main.py. Gateway fb973278 expone GET /api/v1/catalogo/** público
sin reescritura. Web 041dce2a: catálogo api.ts, FichaView y BookingPanel.
Búsqueda acepta texto, categorías repetidas, precio mínimo/máximo, orden y cursor.
La ficha conserva solo campos presentes. Franjas M2 son informativas: el CTA consulta
ofertas M3 usando id de ficha, como BookingPanel; POST usa servicio_ref de la oferta
que M3 devuelve. No se inventa una equivalencia para crear la reserva.

## Dirección visual y revisión

Se mantiene identidad sierra/mar: tinta #1D2B5C, base #FDFBFA, rosa #F4E1DE,
verde #006B3F, oscuro #0D1723, texto #F2F4F7, familia Poppins. Catálogo conserva
encabezado y sus filas; cada fila es ahora una acción táctil hacia resultados.
Un CTA Buscar atractivos hace visible el alcance de la búsqueda real.

Resultados: columna izquierda con título, texto de búsqueda, filtros en hoja y lista
de atractivos reales. Imagen de origen con placeholder neutral, nombre y precio
solo si existe; Cargar más explícito preserva foco y posición. Nada de destinos
ficticios ni carruseles de demostración. Vacío es contenido vacío, no fallo del servicio.

Ficha: título y multimedia real, descripción, categorías, atributos públicos, ubicación
si existe, precios de referencia y disponibilidad informativa. Secciones ausentes no
se dibujan. La acción Ver opciones de reserva consulta la oferta del servicio M3.
Crítica previa: no reemplazar el móvil por una portada web; evitar uniformar todos
los datos en tarjetas. Jerarquía con textos y superficies nativas, fotos reales solo
cuando el backend las suministra. Estados claros/oscuros heredan MaterialTheme.

## Skills adaptadas

frontend-design 1.1.1 leída. modern-web-guidance 1.0.1: búsqueda search results
pagination; guía forms recuperada completa (417 líneas). Etiquetas visibles, búsqueda
GET y acciones nativas, filtros aplicados explícitamente y tap targets de 48dp.
Regla aplicada: "DON'T auto-submit a form when a user changes ... checkbox without
an explicit submit action"; controles de filtros esperan Aplicar. Búsqueda textual
con cancelación/debounce; resultados se anuncian mediante LiveRegionMode.Polite.

## Verificación prevista

MockWebServer verifica rutas, filtros repetidos, cursor, ausencia de autorización,
nullable/metadata y respuesta inválida. ViewModel verifica cancelación, respuesta tardía,
reinicio de cursor, append sin duplicados y retry sin borrar resultados anteriores.
El root integra recursos/navegación/DI y ejecuta la verificación central Fase 2 antes
de activar fases posteriores. Aquí no se ejecutan builds concurrentes.

## Endurecimiento monetario antes de expandir decimales

La entrada de precio admite decimal ordinario, máximo 16 dígitos enteros y 8
decimales, sin exponentes. La longitud cruda se comprueba antes de parsear.
El mapper remoto admite números científicos solo con exponente corto acotado,
precisión máxima 24, hasta 16 enteros y 8 decimales; normaliza una escala negativa
solo después de comprobar el tamaño final. Son límites defensivos del cliente,
no una modificación del contrato o de las tarifas del backend.
Se añadieron pruebas para exponente gigante (sin expandirlo), entrada de longitud
extrema, rechazo antes de requests/SavedState y preservación de decimal exacto.
No se ejecutó Gradle concurrente; verificación central a cargo de root.
