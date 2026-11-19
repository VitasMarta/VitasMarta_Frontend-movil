# Continuidad — estado vigente del trabajo

Última actualización: cierre local del rediseño fotográfico de Explorar,2026-11-18.
Actualizar al terminar cada bloque y antes de agotar contexto. Primero contrastar
este archivo con git status y GitHub: no repetir trabajo ya aprobado sin cambios.

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

## Base y rama

PR1 fusionado por el propietario; main1ff9240876f11b6144c51597b07e6d04dd94e282.
Ambos checks de su código e096f6810dba9dc637918032db08f9e467f7687b aprobados.
Rama de rediseño: feature/explore-visual-redesign. El propietario dijo «Dale» a
mejorar Explorar con portada fotográfica, categorías visuales y fondos.

Consultar git log/status y gh pr view --repo VitasMarta/VitasMarta_Frontend-movil
para saber si el commit/push/PR de esta rama ya se publicó. No inferir que falta
publicarlo solo porque este archivo no guarda un hash de su propio commit.
El PR1 NO es el PR del rediseño. No hacer merge aunque los checks estén verdes.

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

1. Si aún falta publicar esta rama, revisar diff/secretos, commit/push y abrir PR
   normal (sin borrador si ya está listo), con 104 JVM y11 físicas aprobadas.
2. Verificar TODOS los checks del PR actual antes de dar CI por aprobada. Si ya
   fue publicado, no crear un PR duplicado; consultar el existente.
3. Si CI está verde, actualizar descripción del PR con evidencia y comunicar enlace.
   La revisión visual y el merge corresponden al propietario.
4. Seguir instrucciones nuevas del propietario; no ampliar otras pantallas por
   iniciativa propia durante este corte de Explorar.

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
hay tres contenidos públicos, incluido Buffet Hilton en gastronomico; Aventura
vacía no significa que todo esté vacío. Franjas M2 informativas vacías no prueban
ausencia de ofertas M3, que exige sesión. Falta transacción privada real/manual.
Restablecimiento/MFA/OAuth nativo, persistencia de sesión, auditoría accesible
completa y firma de distribución/Play conservan sus pendientes. No declarar
MVP100% ni pruebas privadas reales completadas por tener tests con fixtures.
