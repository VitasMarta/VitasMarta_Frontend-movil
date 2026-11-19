# Perfil y recomendaciones nativos — plan previo

## Fuentes

M7 95f1d32c: PerfilController/ArcoController, DTO snake_case, casos de uso perfil.
M6 904673be: app/api/routes.py y schemas.py. Gateway fb973278 define acceso privado.
Referencias se mantienen sin modificaciones. No se ejecutan mutaciones reales.

## Plan visual y revisión

Se conserva Poppins y la paleta del sistema móvil: tinta #1D2B5C, verde #006B3F,
rosa #F4E1DE, base #FDFBFA; oscuro #0D1723 con texto #F2F4F7. Perfil usa cabecera
breve y filas de acciones que abren formularios específicos, en una columna de
24dp y controles >=48dp. No se replica el dashboard web. Recomendaciones emplea
lista de destinos con explicación y precio solo cuando se conoce; títulos de
sección y avisos de transparencia visibles, sin métricas decorativas.

Crítica: evita una pantalla de perfil enorme con todos los formularios simultáneos.
El resumen ofrece Datos personales / Idioma / Intereses / Privacidad; cada editor
tiene guardar explícito. Los switches de consentimiento conservan el valor remoto
y solicitan confirmación explícita del cambio; no muestran guardado antes de éxito.
Perfilado opcional separado de comunicaciones comerciales. No se ofrece revocación
de tratamiento como un switch inocuo: podría cancelar/revocar la cuenta.

## Contratos y límites concretos

Perfil contiene usuario_ref, nombre_visible, datos_contacto y fecha_actualizacion.
Solo esos campos editables se envían; datos de contacto se preservan al editar el nombre.
Preferencias son strings: CORREO, idioma_autorizado e idioma_preferido es-CO/en-US.
NO existe GET intereses en el main M7: solo PUT. El editor permite reemplazo explícito
con categorías públicas reales, sin afirmar que la selección inicial sea el estado
remoto. Privacidad sí lee mapa vigente/historial de consentimientos. ARCO lista y
detalle se consultan; nuevas solicitudes requieren confirmación y no prometen
eliminación inmediata. Exportación PDF requiere integración de almacenamiento
privado/compartir y se documentará por separado si no entra en esta entrega.

M6 devuelve recomendaciones[], metadatos_ia, modo y advertencias; 206 se mantiene
como éxito degradado. No existe /alternativas/{id}; alternativas_sugeridas son IDs
que pueden abrir ficha con callback. Ausencia de candidatos es un vacío real.

## Skills y adaptación

frontend-design 1.1.1 y modern-web-guidance 1.0.1 leídas. Search accessible profile form,
retrieve forms/switch, 480 líneas completas. Se adaptan etiquetas visibles, foco,
validación al enviar, anuncios de estado y selectores nativos a Compose.
Forms: «DO use actionable language on submit buttons». Switch: «preserves native
form submission, keyboard navigation, and screen reader semantics». Material3
Switch/RadioButton proporcionan estas semánticas sin HTML ni polyfills.

## Verificación planificada

Pruebas JVM de contratos de rutas/body y headers privados, respuestas parciales,
206 y campos opcionales; estados ViewModel, doble envío, errores y limpieza al
cambiar sesión. Compilación/lint/formato centrales secuenciales a cargo de root.
No se da por verificado nada antes de ejecutarlo.

## Implementación preparada para verificación central

Perfil integra lectura y edición de nombre visible sin sobrescribir datos_contacto;
preferencia de idioma solo tras respuesta confirmada; consulta/cambio independiente
de COMERCIAL y PERFILADO; reemplazo explícito de intereses usando categorías públicas
y sin inventar GET intereses. ARCO soporta historial/detalle, ACCESO, rectificación
de nombre_visible, OPOSICION específica y CANCELACION con diálogo de bloqueo,
seudonimización y revocación. CANCELACION solo limpia sesión tras recibo 201.
La exportación usa MIME PDF, streaming privado, límite de 10MiB y lectura IO dentro
de AuthenticatedApiExecutor. Root integra el selector de documento y locale nativo.

M6 consume recomendaciones[] y conserva 206 como contenido degradado, valores
ambientales desconocidos como null, transparencia y callbacks a fichas/alternativas
que provienen de IDs reales. No llama /alternativas/{id}. No muestra precio en la
lista M6 porque ese DTO no publica moneda; la ficha del catálogo aporta detalle.

Ambos ViewModels observan SessionState completo, incluida generación, cancelan
lecturas al cambiar sesión y comprueban cancelación/generación tras suspensiones.
Perfil usa revisión de operación para que el finally de una carga vieja no apague
el spinner de la nueva; retry de detalle ARCO conserva y vuelve a consultar su ID.
No persisten perfiles, recomendaciones ni credenciales. Botón y gesto Atrás regresan
de subsección a resumen, y del detalle de trámite a historial.

Se añadieron 15 pruebas ViewModel significativas (11 perfil, 4 recomendaciones):
confirmación sin optimismo, doble envío, consentimientos independientes, callback de
idioma, intereses sin escritura al seleccionar, cancelación de cuenta, respuesta
tardía resistente a cancelación, misma cuenta con generación nueva, Retry-After,
exportación rechazada, solapamiento de cargas y reintento de detalle ARCO. Las pruebas
contractuales de repositorio perfil/M6 fueron verificadas por root antes de estos
últimos tests. La suite final, formato y lint quedan para el coordinador; no se
ejecutó Gradle concurrentemente ni se hicieron mutaciones reales en el despliegue.

## Revisión de cumplimiento de guías

forms: guardar explícito, validación al enviar y anuncio de éxito confirmado evitan
«double-posts» y cambios silenciosos. switch: Material3 Switch conserva semántica
nativa y estado remoto; el diálogo debe confirmarse antes de enviar. Las guías
web se traducen a controles Android, sin CSS, HTML, WebView o polyfills.

## Frontera monetaria de M6

precio_minimo ausente o null permanece desconocido. Un valor presente no decimal,
negativo o fuera de límites produce InvalidResponse en lugar de ocultarlo como null.
La importación se alinea con catálogo: texto de hasta 64 caracteres, precisión de
hasta 24 dígitos, máximo 16 enteros y 8 decimales, escala aceptada -16..8. Antes
de BigDecimal se comprueba el exponente e/E: hasta tres dígitos y valor -32..32.
Solo tras esas comprobaciones se normaliza una escala negativa a cero, por lo que
1e2 se importa como 100 sin permitir expansiones enormes. Son límites defensivos
de importación del cliente; no representan una restricción adicional del backend.
La lista de recomendaciones sigue omitiendo precios porque M6 no publica moneda.
Se añadió una prueba matricial de ausencia/null, precisión decimal exacta, exponentes
normales y entradas malformadas/extremas. No se ejecutó Gradle desde este subagente.

## Integración central verificada

Exportación y comprobante conectados a selector nativo; cambio de idioma aplicado
con LocaleManager y fallback. Suite central: 104 JVM, lint y formato aprobados,
release con R8 compilada. Nueve instrumentadas aprobadas, una omitida; captura de
recomendaciones es fixture exclusivo de test, no resultado personalizado real.
