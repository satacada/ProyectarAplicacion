# ADR-002: Telemetría de Logs por Red Local para Android TV sin ADB

* **Estado:** Aceptado
* **Fecha:** 2026-09-14
* **Contexto:** La TV Android / TV Box tiene deshabilitada la depuración ADB inalámbrica por restricciones del fabricante. La IA no puede leer logcat directamente vía ADB.
* **Decisión:** Implementar un emisor de logs integrado dentro de :app-receiver que retransmita excepciones y eventos con timestamp vía red local hacia :app-sender o un servidor local en la PC.
