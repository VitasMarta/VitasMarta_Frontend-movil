# Propuesta móvil — Santa Marta, a tu ritmo

Fase 0, 2026-11-17. Propuesta de interfaz nativa, todavía sin aplicación ni capturas
instrumentadas. [Lámina de composición](design/mobile-direction.svg).

## Dirección

El turista debe encontrar una experiencia, entender qué puede visitar y reservar
sin perder contexto. La búsqueda y la fotografía de cada lugar serán protagonistas.
La identidad combina Azul Tinta, Rosa Niebla y Esmeralda existentes; la composición
móvil concentra una decisión por pantalla y mantiene las acciones principales cerca
del pulgar. Textos breves, sin titulares promocionales vacíos ni etiquetas técnicas.

La mejora frente a la web es de interacción: cuatro destinos estables, búsqueda
siempre visible al explorar, filtros en sheet, detalle en una columna y reserva por
pasos. La web actual usa hero oscuro, navegación de escritorio y rejillas; esa
composición no se traslada al móvil. No se añaden favoritos, mapa interactivo,
valoraciones, descuentos ni pagos embebidos sin contratos reales.

## Marca y tokens extraídos

Fuente: web commit `041dce2a2299262eae23c157f1e3d686e314aad8`, src/app/globals.css,
src/app/[locale]/layout.tsx, src/app/icon.svg y gu_a_detallada_de_desarrollo_frontend.md §1.
Se revisaron HomeView.tsx, cards.tsx, SearchBar.tsx y SiteChrome.tsx como referencia
funcional/visual por código. No se ejecutó la web ni se obtuvieron capturas de ella.

| Token | Valor de referencia | Decisión nativa |
|---|---|---|
| Azul Tinta | #1D2B5C | Texto principal, CTA primario, destino seleccionado |
| Rosa Niebla | #F4E1DE | Superficies de énfasis puntual, selección, revisión de reserva |
| Esmeralda | #00A060 | Acento gráfico de sostenibilidad, sin texto pequeño blanco encima |
| Esmeralda texto | #006B3F | Texto semántico accesible en superficies claras |
| Superficie base | #FDFBFA | Lienzo principal continuo |
| Blanco | #FFFFFF | onPrimary y superficies donde su contraste esté comprobado |
| Ámbar / coral | #B45309 / #B91C1C | Avisos ambientales con texto/ícono, nunca solo color |
| Azul de acción actual web | #1A5FD6 | Extraído como referencia; CTA móvil propuesto en Azul Tinta |
| Línea actual web | #D5DAE3 | Separador decorativo; no usar como único límite de un input |
| Poppins | 400/500/600/700 | Familia de marca para Compose; empaquetar fuente y licencia verificadas en fase técnica |

El ícono de hoja existe en src/app/icon.svg: no diseñar otro logo ni distribuir
assets de demostración `public/mock` como fotos reales. La lámina es un esquema
vectorial propio; sus bloques de foto son placeholders explícitos de documentación.
No hay fotografías reales ni fixtures conectados a producción.

## Traducción a Material 3

| Propiedad | Propuesta |
|---|---|
| colorScheme | primary = Azul Tinta, onPrimary = blanco; secondary = Esmeralda texto; surface = base; onSurface = tinta; error = coral |
| Tipografía | headlineLarge 32sp/38sp 700, headlineMedium 26sp/32sp 600, titleLarge 22sp/28sp 600, bodyLarge 16sp/24sp 400, labelLarge 14sp/20sp 600 |
| Espaciado | Escala 4, 8, 12, 16, 24, 32dp; margen de pantalla 24dp, 16dp en ancho muy reducido |
| Forma | Imágenes abiertas con recorte 12dp si facilita continuidad; controles 12dp; sheet 24dp superior. Evitar sombras y cajas repetidas |
| Acciones | Altura mínima 48dp; CTA principal 56dp mínimo, ampliable con texto; ningún contenedor de texto de alto fijo |
| Superficies | Contenido en lienzo continuo; separadores y espacio marcan agrupación, no una rejilla desktop de tarjetas |
| Movimiento | Responde a acciones (sheet, paso, confirmación); sin entrada animada de cada fila ni autoplay; respetar escala de animación del sistema |
| Adaptación | NavigationBar compacta; NavigationRail si hay ancho; contenido limitado en pantallas grandes sin agrandar tipografía arbitrariamente |
| Tema oscuro | Pendiente de validación visual y tokens de contraste; no declarar soporte con inversión automática |

Tokens de color auditables: [design/mobile-tokens.json](design/mobile-tokens.json).
La lámina usa fuente de sistema como fallback para poder abrirse sin conexión;
no es una captura de Compose ni valida la integración final de Poppins.

## Composición e información

**Explorar.** Marca compacta, título contextual “Santa Marta, a tu ritmo”, campo de
búsqueda con etiqueta persistente, categorías reales en FilterChip y botón Filtros.
Lista vertical de destinos con fotografía amplia y nombre/precio/categoría solo
cuando existan. Mantener un único objetivo visual por sección. La ausencia de foto
muestra un placeholder honesto, sin foto de otro lugar. La ausencia de precio no
se interpreta como gratis. Actualizaciones del listado preservan foco y posición.

**Detalle.** Fotografía del destino, volver accesible, nombre y datos presentes.
Descripción/atributos en una columna legible. Información ambiental separada del
estado comercial. CTA inferior “Ver horarios” conserva insets y no tapa contenido.
Si no hay sesión, login contextual conserva el id y vuelve al flujo. Las franjas
vendibles se obtienen de M3: el catálogo no certifica cupos ni políticas para reservar.

**Reserva.** Horario y cupos → revisión de política y total → resultado del servidor.
Número de paso solo aquí, porque existe una secuencia real. Selección por franja_ref,
no por texto de hora. Aceptación independiente y explícita de la versión vigente.
Envío deshabilita la acción tras validar; no dejar deshabilitado el formulario vacío
sin explicar sus errores. El resultado usa estado/vence_en/siguiente_accion reales;
una reserva pendiente no se presenta como confirmada.

**Mis reservas.** Segmentos Activas/Pasadas/Canceladas con estado en texto y fecha.
El detalle permite comprobante/cancelación solo si los contratos y estado lo permiten.
Motivo y penalidad en confirmación contextual. Error de red ambiguo conserva la intención.

**Para ti.** Explicación y aviso de IA devueltos por el servidor, acceso claro a privacidad.
206 muestra contenido con aviso discreto de sugerencias generales. Sin sesión,
pedir acceso antes de llamar M6; no mostrar una recomendación ficticia para rellenar.

**Perfil.** Datos personales, idioma, intereses, privacidad y derechos como destinos
breves. Consentimientos comerciales/perfilado separados; no afirmar eliminación
instantánea cuando el backend solo registra solicitud ARCO.

La barra compacta propone Explorar, Reservas, Para ti y Perfil. Etiquetas siempre
visibles, selección semántica y contraste comprobado. El grafo conserva estado
no sensible por destino. Flujos detallados en SCREEN_MATRIX.md.

## Aplicación de frontend-design

Skill local leída, plugin 1.1.1 (ruta y hash en PHASE_0.md). Se aplicó su proceso de
planificar, revisar y criticar: el sujeto es turismo sostenible en Santa Marta y
la tarea principal es encontrar y reservar. Se concentra la expresión visual en
la fotografía real del destino y tipografía de marca; el resto facilita lectura.
Se evita numeración decorativa, mayúsculas sistemáticas, colores arbitrarios,
animaciones dispersas y copy sin propósito. El diseño conserva datos contractuales,
con interfaz propia para interacción táctil.

## Modern Web Guidance

- **`accessibility`**: apliqué “Don't use placeholders as primary labels” a la propuesta:
  TextField tendrá label persistente; encabezados con semantics.heading(), controles
  Material con roles/selección y contentDescription solo cuando aporte información.
  No duplicar la etiqueta hablada de un icono ya acompañado de texto.
- **`forms`**: apliqué “DO disable the button *after* a valid submission is clicked to
  prevent double-posts” al flujo de reserva. Validación al enviar/salir del campo,
  corrección mientras se edita, foco al primer error y bloqueo de envío en ViewModel.
  La protección real frente a duplicados usa además la clave que emite M3.

Adaptación nativa: KeyboardOptions para correo/password/IME; autofill mediante APIs
Compose compatibles con la versión elegida; semántica error/stateDescription y
liveRegion solo para actualizaciones relevantes. Teclado y navegación por foco
se verifican con TalkBack y teclado físico. Dialog/ModalBottomSheet nativos gestionan
foco; no se copian focus traps DOM. No trasladar CSS, ARIA, CSRF de cookies de navegador,
Baseline, DOMPurify ni unidades px a la app. Campos con portapapeles y gestores de claves
permitidos. Objetivos táctiles mínimos 48dp y texto en sp, sin escalado bloqueado.

Fuentes nativas consultadas: [valores de accesibilidad de Compose](https://developer.android.com/develop/ui/compose/accessibility/api-defaults),
[semántica Compose](https://developer.android.com/develop/ui/compose/accessibility/semantics)
y [arquitectura Android](https://developer.android.com/topic/architecture).

## Estados y revisión pendiente

Todas las pantallas de datos deben cubrir carga, contenido, vacío, error recuperable,
sin conexión y actualización; degradación donde existe contrato. Los formularios añaden
validación inline, envío y confirmación. La pantalla principal no depende de que M6/M8 respondan.
Se anuncian cambios relevantes, sin leer “cargando” en cada tecla de búsqueda.

Verificación documental: identidad de tokens contrastada contra CSS y fuente reales;
contraste calculado en sRGB para pares declarados; SVG parseado y revisado visualmente.
Esto no certifica accesibilidad de la app. Pendiente: Compose en teléfono pequeño/grande,
200% de texto, TalkBack, teclado abierto, claro/oscuro si se implementa, restauración y Back.
No se ha ejecutado ninguna inspección en emulador/dispositivo.

## Actualización: diseño nativo y apariencia verificados

La dirección implementada y sus capturas físicas están en
[NATIVE_REDESIGN.md](design/NATIVE_REDESIGN.md). Incluye un diseño propio de Android
y los modos Sistema/Claro/Oscuro persistentes; reemplaza las menciones previas a
una futura implementación del modo oscuro. Identidad sierra/mar, Poppins y superficies
semánticas; 34 pruebas JVM y tres Compose físicas aprobadas.
