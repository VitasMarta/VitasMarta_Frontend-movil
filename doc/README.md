# VitasMarta — Frontend móvil Android

Documentación base para desarrollo asistido por Codex. Copiar estos archivos a la raíz de VitasMarta_Frontend-movil sin sobrescribir trabajo existente.

## Referencias de verdad
- Gateway M1: https://github.com/VitasMarta/servicio_api_gateway
- Organización: https://github.com/orgs/VitasMarta/repositories
- Web: https://github.com/VitasMarta/VitasMartaFrontend
- Contratos: https://github.com/VitasMarta/VitasMartaFrontend/blob/main/contratos_de_consumo_api_del_frontend.md
- Guía: https://github.com/VitasMarta/VitasMartaFrontend/blob/main/gu_a_detallada_de_desarrollo_frontend.md
- Configuración: https://github.com/VitasMarta/VitasMartaFrontend/blob/main/.env.example
- Android: https://developer.android.com/topic/architecture

## Inicio
Instalar Android Studio, SDK, JDK compatible con AGP y Git. Clonar los tres repositorios como hermanos, incluyendo `servicio_api_gateway`; usar credenciales Git autorizadas si se requieren. Ejecutar Codex desde la raíz móvil y darle acceso de lectura a las referencias. Verificar skills locales y URL HTTPS real antes de integrar. Inicializar Gradle wrapper si no existe. No copiar archivos .env con credenciales.

## Alcance propuesto
MVP turista primero; prestadores después; administración y monitoreo quedan sujetos a aprobación. No se presume paridad completa con la web. Confirmar si se requiere otro rol para la primera entrega. Android solamente; iOS fuera de alcance.

## Documentación
- AGENTS.md: reglas del agente.
- docs/ARCHITECTURE.md: arquitectura y convenciones.
- docs/API.md: integración y verificación contractual.
- docs/DESIGN.md: experiencia visual y skills.
- specs/REQUIREMENTS.md: requisitos y criterios de aceptación.
- specs/TASKS.md: plan verificable.
