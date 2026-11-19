# Continuidad — estado vigente del trabajo

Última actualización: integración de las cuatro fotos en Explorar.
Rama `feature/category-photographs`, base `51c4f59` (PR2 fusionado por el propietario).
Contrastar este registro con Git y GitHub antes de continuar.

## Instrucciones del propietario

- Trabajar exclusivamente en VitasMarta_Frontend-movil. Leer AGENTS.md/doc/AGENTS.md.
- Kotlin/Compose, Clean Architecture/MVVM y feature-first; diseño propio, no copiar web.
- Gateway/web y demás repositorios solo lectura; referencias dentro .git/vitasmarta-references/.
- SIN subagentes (última preferencia explícita).
- Autorizados local, fetch/pull/commit/push/PR; el agente NO hace merge.
- HTTP autorizado solo al origen local del Gateway; no publicar su URL o secretos.
- No pedir credenciales, extraer tokens ni automatizar reservas/cambios privados reales.
- Avisar cuando se necesite teléfono USB; no modificar ajustes de seguridad.
- Skills frontend-design y modern-web-guidance obligatorias, adaptadas a Compose.

## Fotografías de categorías integradas

### Asignación exacta

| Imagen del mensaje | Contenido | Categoría / slug técnico | Original permanente |
|---|---|---|---|
| #1 | Catedral blanca | Cultura / `cultura` | [cultura.png](../design/category-references/cultura.png) |
| #2 | Sendero y terrazas entre vegetación | Aventura / `aventura` | [aventura.png](../design/category-references/aventura.png) |
| #3 | Playa con mirador de techo de palma | Ecoturismo / `ecoturismo` | [ecoturismo.png](../design/category-references/ecoturismo.png) |
| #4 | Pescado, patacones y arroz | Gastronomía / `gastronomico` | [gastronomia.png](../design/category-references/gastronomia.png) |

Los originales están copiados byte a byte dentro del repositorio. No depender
de /tmp/codex-clipboard-*; pueden desaparecer al cambiar de sesión. No inventar
licencias/autorías de estos adjuntos ni reutilizar los créditos Commons anteriores
para atribuirles autoría. La procedencia conocida es «aportadas por el usuario».

### Implementación y evidencia

- Las cuatro copias WebP conservan el tamaño de sus originales y suman 352 518 bytes.
- Las tarjetas usan una proporción 1,65:1, con un máximo de 380 dp para la
  fila de una columna. Cultura ancla arriba, Aventura abajo, Ecoturismo al
  centro y Gastronomía cerca del centro con un leve ajuste hacia el plato.
- Se conservaron los slugs, etiquetas de prueba, acciones y el icono para
  categorías desconocidas. Los créditos de las cuatro fotos indican su
  procedencia y se conservan las atribuciones existentes.
- `verify-android.sh` aprobó la compilación de ambos APK, las 104 pruebas JVM,
  lint y ktlint. Log local en `.local-tools/category-verify.log`.
- Se revisaron recortes de referencia a anchos representativos de una, dos y
  tres columnas. En el Xiaomi se revisaron las nuevas fotografías en dos
  columnas, en claro y oscuro. El caso de 320 dp y letra al 200 % comprueba
  el nombre largo y el icono de respaldo mediante su fixture existente.
  Las tres columnas se revisaron mediante recortes de referencia.
- Runner físico: 12 casos, 11 aprobados y 1 omitido (requiere origen vacío),
  cero fallos. Log y capturas en `.local-tools/category-device.log` y
  `.local-tools/category-captures/`.

La foto del plato corresponde a la categoría Gastronomía. Las imágenes de
restaurantes individuales dependen del catálogo del Gateway.

## Implementado y comprobado localmente

- ExploreArtwork.kt: hero con foto, botón, arte por categoría y créditos.
- CatalogScreen.kt: fondo, ancho máximo1120dp, cuadrícula1/2/3 columnas; a menos
  de360dp o fontScale>=1.5 una columna y controles de búsqueda/filtro apilados.
- Fotografías WebP locales: explore_tayrona.webp/explore_bay.webp,132KB totales.
- Etiquetas/traducciones:265 claves en cada uno de los tres recursos de idioma.
- Canvas decorativo recortado: no debe invadir los nombres de categorías.
- Hero capa .62–.76: contraste mínimo calculado5.07:1 sobre foto blanca.
- Categorías SOLO de Gateway; testTag catalog-category-<slug> y callbacks preservados.
- No se modificaron ViewModels, repositorios, contratos, backend ni autenticación.

Plan/crítica y capturas: ../design/EXPLORE_VISUAL_REDESIGN.md.
Fuentes/licencias: ../licenses/EXPLORE_IMAGES.md y app/src/main/assets/licenses/explore-images.txt.
Tayrona: Felipe Restrepo Acosta, CC BY-SA4.0, fuente Commons enlazada en UI.
Bahía El Rodadero: Petruss, dominio público. Derivados redimensionados/comprimidos,
licencias y créditos conservados. No sustituir esas fotos por imágenes sin licencia.

## Pruebas — YA APROBADAS

verify-android.sh BUILD SUCCESSFUL tras última corrección:104 JVM sin fallos,
lint/ktlint aprobados y APK de pruebas compilado. Log ignorado:
.local-tools/explore-verify-reviewed.log.
Runner físico directo:12 casos,11 aprobados,1 omitido por requerir origen vacío,
0 fallos; «OK (12 tests)». .local-tools/explore-device-reviewed.log y counts.json.
Capturas finales en .local-tools/explore-reviewed-captures/ y copiadas a doc/design/
como device-photo-*.png. Revisadas claro/oscuro y320dp con letra200%.
Primera revisión detectó buscador estrecho y decoración invadiendo el texto;
corregidos y repetidas las pruebas. No usar las capturas iniciales como finales.
No hay Gradle/runner activo al cerrar la verificación. App principal/auxiliar
instaladas y MainActivity abierta. No repetir instalación/pruebas sin necesidad.
Tablet/horizontal/TalkBack exhaustivo siguen sin certificación; no prometer todas
las pantallas verificadas. El fixture de categoría larga existe solo en androidTest.

## Siguiente paso concreto

Publicar el PR de esta rama y comprobar CI. El propietario realiza el merge.

## Herramientas

JDK17 /usr/lib/jvm/java-17-openjdk; SDK ignorado .local-tools/android-sdk.
local.properties ignorado contiene SDK/origen privado; NUNCA copiarlo a Git.

```sh
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./scripts/verify-android.sh :app:ktlintFormat
env JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./scripts/verify-android.sh
```

Formato primero y separado. No Gradle concurrente. Script fija GRADLE_USER_HOME
y ANDROID_USER_HOME ignorados. SDK36,BuildTools35,Gradle8.13,AGP8.13.2,Kotlin2.1.20.
ADB .local-tools/android-sdk/platform-tools/adb. Dispositivo Xiaomi2201122G Android15.
INSTALL_FAILED_USER_RESTRICTED requiere aceptar el aviso en el teléfono; no es
autorización para desactivar seguridad. Principal y auxiliar pueden pedir dos avisos.

Runner: co.vitasmarta.mobile.debug.test/androidx.test.runner.AndroidJUnitRunner,
gatewaySmoke=true, clase co.vitasmarta.mobile.NativeUiTest. Estado0 aprobado,
-4 assumption skip, -3 ignored, -1/-2 fallos. No confundir parser fallido con app.
Capturar solo nuestra app; comprobar mtime del teléfono antes de recuperar PNG
para evitar recoger screenshots de una versión anterior en su caché.

## CI y skills: detalles que evitan repetir errores

CI: setup-android packages=platform-tools, NO tools(retirado). SDK aislado bajo
runner.temp en env DEL STEP; contexto runner no existe en env del job. Exporta
ANDROID_HOME/ANDROID_SDK_ROOT para pasos siguientes. No desactivar lint, crear
baseline o suprimir defectos. Un run previo falló descargando Maven, el del mismo
commit pasó; reejecución del fallido pasó. Investigar logs antes de cambiar código.

frontend-design1.1.1:
/home/juane/.claude/plugins/marketplaces/claude-plugins-official/plugins/frontend-design/skills/frontend-design/SKILL.md
modern-web-guidance1.0.1:
/home/juane/.codex/plugins/cache/googlechrome/modern-web-guidance/1.0.1/skills/modern-web-guidance/SKILL.md
Guías completas leídas: deliver-optimized-decorative-images y color. La entrega
que aplique guías termina con Modern Web Guidance: un bullet en primera persona
por guía y cita corta EXACTA de una regla, no la introducción. Volver a recuperar
la guía si se perdió la cita; no inventar IDs o citas tras compactar contexto.

## Contexto funcional pendiente de la entrega anterior

REPOSITORY_RECHECK_2026-11-18.md sustituye el diagnóstico antiguo de catálogo vacío:
la auditoría inicial detectó tres contenidos, pero una lectura posterior devolvió
más restaurantes gastronómicos sin foto. Son observaciones fechadas, no un
inventario fijo. Una categoría vacía no significa que todo esté vacío. Franjas M2 informativas vacías no prueban
ausencia de ofertas M3, que exige sesión. Falta transacción privada real/manual.
Restablecimiento/MFA/OAuth nativo, persistencia de sesión, auditoría accesible
completa y firma de distribución/Play conservan sus pendientes. No declarar
MVP100% ni pruebas privadas reales completadas por tener tests con fixtures.
