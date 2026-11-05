# Bitácora de uso de IA

> Completa y corrige esta bitácora con tu propia experiencia. La sección 11 del enunciado exige
> transparencia, y en la defensa deberás poder reproducir o modificar cualquier método en vivo.

| Fecha | Herramienta | Propósito | Prompt resumido | Qué se aceptó / rechazó y por qué |
|---|---|---|---|---|
| 2026-10-28 | Claude (Anthropic) | Generar el proyecto completo | "Crea todo el sistema listo para descargar" a partir del PDF y los .java del esqueleto | Se generaron el modelo (no entregado en el esqueleto), la implementación de los TODO E11–E16, la UI Swing, `pom.xml`, scripts, README y `datos.db`. **Pendiente: indicar qué revisaste, qué cambiaste y qué aún no entiendes.** |

## Verificación realizada con la herramienta

* Compilación con `-Xlint:all`: sin advertencias.
* PruebasModelo T01–T10: aprobadas. T11–T14 requieren el driver SQLite y **no pudieron ejecutarse** en el
  entorno donde se generó el código; deben ejecutarse en tu equipo con `mvn -q exec:java`.
