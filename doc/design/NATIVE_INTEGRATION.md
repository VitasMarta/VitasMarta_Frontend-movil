# Integración móvil del flujo turista — plan previo al código

El propietario autoriza completar catálogo, reservas, perfil y recomendaciones y
dividir el trabajo con subagentes. El árbol de trabajo continúa exclusivamente en
VitasMarta_Frontend-movil; las referencias auditadas de otros repositorios son de lectura.

## Dirección y revisión previa

Se conserva el sistema nativo sierra/mar: Poppins, tinta #1D2B5C, rosa #F4E1DE,
verde #006B3F, base #FDFBFA, oscuro #0D1723 y texto #F2F4F7. Navegación inferior
de cuatro destinos; contenido secundario mediante pila con Atrás visible. No se
añaden dashboards ni indicadores decorativos. La composición se adapta a cada
función: explorar/filtrar, leer ficha, elegir oferta y confirmar condiciones, gestionar
perfil; no se reutiliza una pantalla vacía genérica para todas las funciones.

La revisión del plan exige que una pestaña sin datos muestre vacío, que un fallo
muestre recuperación y que una función no conectada no finja caída de servidor.
Las categorías ahora conducen a búsqueda remota; no se presentan atractivos sintéticos.
No se inventa una ruta de alternativas que M6 no implementa.

## Adaptación de skills

frontend-design 1.1.1 se aplica al diseño móvil existente y las propuestas de cada
feature. modern-web-guidance 1.0.1: stack-drill-down recuperada y leída completa
(734 líneas). La regla “include an explicit back button” se adapta a NavController
y callbacks Atrás; el historial del sistema y los botones usan la misma pila.
Cada pantalla secundaria obtiene título accesible y conserva solo argumentos pequeños.
No se introduce HTML/WebView ni animación manual para simular navegación Android.

## Integración y responsabilidades

- Catálogo: callbacks de categoría/búsqueda/detalle; ficha.id consulta ofertas M3.
- Reservas: guard de sesión preserva el destino previsto; oferta elegida aporta el
  servicio_ref y la franja_ref para crear, nunca se deducen del slug de categoría.
- Perfil y recomendaciones: repositorios autenticados, limpieza de estado al logout,
  cambio real de idioma tras confirmación del servidor, exportación mediante selector
  de documento Android con acción explícita del usuario.
- Raíz: rutas/DI/recursos/instrumentadas; barra seleccionada coherente en subpantallas.
  Se elimina guardar pilas privadas al cerrar sesión y se limpian destinos anteriores.

Los subagentes preparan código en paralelo por solicitud explícita. Integración y
verificación se hacen en orden: catálogo Fase2, reservas Fase3 y perfil/recomendaciones
Fase4. Cada corte requiere compilación, unitarias y lint antes de activarse el siguiente.
No se ejecutan builds Gradle simultáneos. Las pruebas no crean reservas reales ni
modifican perfil/consentimientos reales del despliegue automáticamente.
