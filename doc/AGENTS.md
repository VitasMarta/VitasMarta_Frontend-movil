# AGENTS.md — VitasMarta Android

## Misión y reglas obligatorias
Construir un cliente Android nativo Kotlin/Jetpack Compose de VitasMarta, conectado exclusivamente al API Gateway M1 para operaciones de negocio. El frontend web es referencia funcional y visual, no código para portar literalmente. No modificar repositorios externos sin autorización.

Antes de editar, leer README.md, docs/ARCHITECTURE.md, docs/API.md, docs/DESIGN.md, specs/REQUIREMENTS.md y specs/TASKS.md. Inspeccionar el estado de Git y el proyecto real. Mantener specs actualizados con evidencia de ejecución. No declarar una tarea terminada si no se verificó.

## Referencia obligatoria del Gateway
Inspeccionar `../servicio_api_gateway` (https://github.com/VitasMarta/servicio_api_gateway) en modo lectura: rutas, prefijos, filtros, autenticación, configuración por ambiente y contratos expuestos. Contrastar con el frontend web y documentar discrepancias sin inventar endpoints. Si no hay acceso, registrar bloqueo.

## Skills obligatorias
Buscar las skills instaladas `front-desing` (también comprobar `frontend-design` o `front-design` si ese nombre no existe) y `modern-web-guidance`, leer sus SKILL.md y seguir sus instrucciones aplicables. Documentar ruta y nombre real en specs/TASKS.md. Si faltan, informar al usuario; no inventar su contenido ni instalarlas sin autorización. Adaptar los principios de diseño visual, accesibilidad y UX a Compose/Material 3; no introducir React, CSS, HTML ni WebView como sustituto de UI nativa. Las reglas de seguridad y contratos del proyecto prevalecen.

## Flujo por tarea
1. Leer contrato y casos de aceptación; identificar cambios mínimos y bloqueos.
2. Implementar una funcionalidad vertical completa (DTO, repositorio, ViewModel, UI y pruebas según aplique).
3. Ejecutar `./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug` y comprobaciones de estilo configuradas. Ejecutar pruebas instrumentadas cuando haya dispositivo.
4. Revisar diff, actualizar documentación y marcar solo tareas verificadas. Informar comandos, resultados, riesgos y cambios pendientes.

## Convenciones
Código Kotlin idiomático, nombres en inglés, textos de interfaz en recursos traducibles, contratos JSON con nombres originales mediante serialización. Sin secretos, tokens, PII ni URLs internas en Git/logs. Sin endpoints inventados, mocks en release, excepciones silenciadas, lógica de negocio en composables ni dependencias circulares. Preferir soluciones simples, inyección de dependencias y composición. No introducir abstracciones sin necesidad.

## Git
Ramas `feature/<tema>`, `fix/<tema>`, `docs/<tema>`; commits convencionales y pequeños. PR contra rama de integración existente o main según política del repositorio. No push, merge, cambios destructivos ni publicación sin aprobación. No sobrescribir cambios ajenos.

## Decisión explícita del propietario — 2026-11-18

El propietario pidió aceptar HTTP porque el equipo mantendrá ese transporte. Esta
instrucción sustituye las restricciones previas de HTTPS exclusivo en los documentos.
Aceptar HTTP o HTTPS únicamente al origen Gateway configurado (esquema, host y puerto),
y rutas /api/v1/. Android debe habilitar cleartext solo para el host HTTP configurado,
sin subdominios; no habilitarlo globalmente. La URL sigue en configuración local ignorada.
No desactivar validación TLS para HTTPS ni modificar el backend. HTTP permite observar
y alterar contraseñas, tokens y datos en tránsito; no presentarlo como conexión cifrada.
