# Especificación funcional y aceptación

Alcance provisional: priorizar turista por ser el flujo más natural de una app móvil turística. Confirmar con propietario antes de excluir otros roles de la entrega definitiva. Requisitos de web no equivalen automáticamente a alcance móvil.

| ID | Prioridad | Requisito | Aceptación verificable |
|---|---|---|---|
| MOB-01 | P0 | Explorar catálogo público | Sin sesión, carga categorías, resultados reales, filtros y paginación; maneja vacío/error. |
| MOB-02 | P0 | Detalle de atractivo | Muestra campos presentes, multimedia y disponibilidad; no inventa atributos. |
| MOB-03 | P0 | Autenticación | Login válido/inválido, registro/recuperación según contrato, expiración, refresh concurrente y logout probados. |
| MOB-04 | P0 | Crear reserva | Selecciona franja real, valida datos, maneja conflicto y doble toque; éxito solo con confirmación del backend. |
| MOB-05 | P0 | Consultar reservas | Usuario ve exclusivamente sus reservas; detalle y cancelación si contrato lo permite. |
| MOB-06 | P1 | Recomendaciones | Muestra resultado personalizado o degradado, transparencia y alternativas cuando aplica. |
| MOB-07 | P1 | Perfil y privacidad | Edita campos permitidos, idioma y consentimiento; soporta derechos y eliminación de cuenta según contrato/requisitos de distribución. |
| MOB-08 | P2 | Prestador | Inventario de funcionalidades y permisos antes de desarrollar; flujo separado por rol. |
| MOB-09 | P2 | Administración/monitoreo | Decisión de alcance explícita; no añadir por defecto. MFA obligatorio si contrato lo requiere. |
| NFR-01 | P0 | Seguridad | HTTP o HTTPS al origen Gateway configurado, sin secretos/logs sensibles, autorización del servidor, logout limpia datos. HTTP autorizado por el propietario el 2026-11-18; no ofrece cifrado del transporte. |
| NFR-02 | P0 | Calidad | Build, unitarias, lint y pruebas de flujos críticos exitosos; evidencias en TASKS. |
| NFR-03 | P0 | UX | Estados completos, TalkBack, escalado y navegación atrás; no bloqueos de UI por red. |
| NFR-04 | P1 | Rendimiento | Medir arranque y respuesta UI en dispositivo representativo; listas paginadas e imágenes optimizadas. No prometer SLA sin medición. |

## Definition of Done
Contrato confirmado, código revisado y sin mocks de producción, criterios satisfechos, pruebas de éxito/error, documentación sincronizada y compilación correcta. Si falta endpoint, credencial, equipo o entorno, marcar bloqueado y no completado. La capacidad concurrente es responsabilidad del backend y requiere prueba de carga independiente; no se garantiza mediante este cliente.
