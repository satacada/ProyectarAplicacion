# ADR-001: Definición de Stack y Oráculo de Verificación

* **Estado:** Aprobado
* **Fecha:** 2026-09-14
* **Contexto:** Definición del ecosistema técnico para proyectarAplicacion.

## Decisiones
1. **Stack Principal:** Android / Kotlin / Gradle Dual App (app-receiver y app-sender).
2. **Comando Oráculo de Verificación:** .\gradlew.bat clean build
3. **Regla de Gobernanza:** Ningún cambio de IA o humano se acepta si el comando oráculo no devuelve código de salida 0.
