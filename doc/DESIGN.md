# Diseño y experiencia

## Proceso obligatorio con skills
Leer front-desing (verificar nombre exacto y variantes frontend-design/front-design) y modern-web-guidance antes de proponer UI. Anotar hallazgos, decisiones y limitaciones. Usar principios aplicables a native mobile (jerarquía, composición, accesibilidad, estados y consistencia). No aplicar mecánicamente implementaciones web.

## Referencia visual
Inspeccionar componentes, estilos, fuentes, assets y capturas del frontend web. Extraer tokens reales antes de elegir colores. Crear MaterialTheme con colorScheme, typography, shapes y dimensiones propias; soporte claro/oscuro si se valida visualmente. Mantener marca VitasMarta y adaptar la interacción táctil. No copiar interfaz desktop ni inventar logos. Usar recursos con licencia adecuada.

## Navegación propuesta turista
Barra inferior: Explorar, Reservas, Recomendaciones, Perfil (validar alcance y IA disponible). Inicio público sin sesión si el contrato lo permite. Pedir autenticación al entrar en operaciones privadas, conservar destino previsto. Detalle con galería, información disponible, franjas y CTA principal. Flujos de reserva en pasos breves con confirmación real. Prestadores tendrán navegación separada por rol si se aprueba.

## Estándares
Diseño edge-to-edge con insets, teclado y áreas seguras. Objetivos táctiles de 48 dp, contraste WCAG AA como objetivo, TalkBack, etiquetas semánticas, escalado de texto, foco y estados no dependientes solo de color. Listas lazy, imágenes Coil con placeholder y error, recursos es-CO/en-US, formatos locales. No textos hardcoded. No pedir ubicación hasta que el usuario use función que la necesita. Ofrecer búsqueda manual sin ubicación.

## Estados obligatorios por pantalla
Carga inicial, contenido, vacío, error recuperable, sin conexión, actualización y estados degradados donde aplique. Formularios con validación inline, envío en progreso, prevención de duplicados y confirmación. No presentar datos simulados como reales. Crear previews con fixtures sintéticos exclusivamente para debug/test.

## Revisión visual
Capturas de pantallas principales en dispositivo pequeño y grande, claro/oscuro si soportado, textos largos, teclado y error. Verificar navegación atrás y restauración de estado. Registrar resultados con evidencia; no afirmar que se ejecutó una inspección si no hay emulador.


## Propuesta nativa — Fase 0

La dirección concreta y la adaptación de ambas skills están en
[MOBILE_DESIGN.md](MOBILE_DESIGN.md), con tokens extraídos, navegación, accesibilidad,
estados y [lámina vectorial propia](design/mobile-direction.svg). La inspección de marca
se hizo por código/estilos/assets web; no se copió su composición ni se afirma una
revisión en dispositivo. La app y su tema Compose aún no están implementados.
