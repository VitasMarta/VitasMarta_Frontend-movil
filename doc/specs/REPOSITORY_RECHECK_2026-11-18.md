# Revisión posterior de main y contenidos publicados

Consulta 2026-11-18, 20:44 America/Bogota. El propietario informó que hay eventos
publicados. Se repitió fetch de main en las doce copias de referencia; no se
cambió su árbol de trabajo ni se modificó ningún servicio.

## Resultado vigente

El Gateway ahora devuelve tres contenidos públicos con la misma consulta que usa
la app: GET /api/v1/catalogo/busqueda?orden=relevancia&descendente=false&limite=20.
La observación de cero resultados en la auditoría inicial no describe este corte.
No se ha demostrado cuándo cambió el contenido ni qué commit está desplegado.

| Contenido devuelto | Categoría |
|---|---|
| Quinta de San Pedro Alejandrino | cultura |
| Museo Tairona UI c5ea97-0v5w | cultura |
| Buffet mediterráneo semanal (Hilton Garden Inn) | gastronomico |

La categoría Aventura elegida por la prueba anterior no contiene estos registros.
Buscar sin filtro devuelve tres; filtrar gastronomico devuelve uno. No se deben
confundir categorías con tipos editoriales (evento/actividad/atractivo). Se mantiene
la taxonomía de seis slugs reales; no se añade una categoría eventos inventada.

Los tres GET de ficha con idioma=es-CO respondieron 200 con nombre, descripción,
categorías, atributos y rango_precio; multimedia opcional. Los tres GET /franjas
respondieron 200 con franjas=[] en esta lectura. Es disponibilidad informativa de
M2: no prueba ausencia de ofertas M3, que requiere sesión. No se consultó M3 con
credenciales ni se efectuó ninguna reserva. Publicado y reservable son estados
diferentes que el móvil determina desde sus servicios respectivos.

## Commits main consultados

| Repositorio | SHA |
|---|---|
| VitasMartaFrontend | `6d9f15f289b7c0fe9bc40f60ad299ad58df9bdc5` |
| VitasMarta_Frontend-movil | `d49342fb9bfcfb0beb831966e513e787d1b44dbf` |
| servicio_ambiental | `3cb14d6b630f5c6cad4a06d129eb955a6817cbd5` |
| servicio_api_gateway | `a95f38cfb517b25aaf6639d38e7f64cca6a31d6b` |
| servicio_catalogo | `c71ad9027af5683aefe31c7b0616d84bbafba6c2` |
| servicio_identidad | `95f1d32c5bebb95ea9fbd864330275b4994e05e1` |
| servicio_integracion | `3e7129e2b6f29145d3e6ec6d129f80bc589657f2` |
| servicio_notificaciones | `f8f04b8250e577433a149a99afc13f20df25a377` |
| servicio_prestadores | `daef49b3f7c0f94bf835d119c4e03f7da20d9cdd` |
| servicio_recomentaciones_IA | `904673bee7f0999801d45fc2784a6b43471c11ec` |
| servicio_reportes | `b05d3b963b86840eb38257a95531b1b92169eb21` |
| servicio_reservas | `49fe4fb9afe38d6d236aad630bafcae55a644b01` |

Cambios respecto al primer corte: Gateway a95f38c configura rutas de salud por
servicio; no cambia las rutas de catálogo/reservas ni la autenticación. Prestadores
daef49b solo añade documentación de redespliegue conservando volúmenes. Web
6d9f15f incorpora moderación, pipeline, enlaces de correo y consulta de ficha con
idioma; el móvil ya envía idioma explícito. Notificaciones f8f04b8 configura el
portal de enlaces de correo. Catálogo y reservas conservan sus commits anteriores.
No se ejecutaron suites de otros repositorios ni se certifica su CI/despliegue.

## Verificación móvil

104 JVM aprobadas, lint/formato y debug/release aprobados. Diez casos físicos
ejecutados: nueve aprobados y uno omitido por requerir origen vacío, cero fallos.
Se añadió después una prueba de listado publicado → ficha real → acceso a reserva;
compilación y ktlint aprobados. Su ejecución específica quedó bloqueada por
«No connected devices!» después de que el propietario se ausentara; cero casos
ejecutados en ese intento. No se presenta como prueba aprobada.
La versión debug verificada quedó instalada y se abrió correctamente antes de
la desconexión. Al regresar el propietario, probar Buscar atractivos o Gastronomía
y acceder a Ver opciones de reserva tras iniciar sesión, sin inventar datos.
