# AGENTS.md — GUÍA DE INSTRUCCIONES PARA IA (Screen Mirroring - proyectarAplicacion)

Este repositorio sigue el estándar abierto AGENTS.md para el desarrollo de la app nativa Android de Screen Mirroring.

---

## 1. NATURALEZA DEL PRODUCTO: SCREEN MIRRORING
- **Dominio Principal:** Transmisión de video/audio en tiempo real, baja latencia y gestión de sockets de red.
- **Tolerancia a Red:** Toda reconexión de socket o pérdida de paquetes debe manejarse de forma silenciosa sin cerrar la aplicación (Principio Fail Fast).
- **Telemetría en TV:** Ante fallos en la Android TV, analizar el log del streaming enviado por red local con timestamps ('HH:mm:ss.SSS').

---

## 2. ESTRATEGIA DE PRUEBAS
- **Unidades Nativas:** Probar codificación/decodificación y formateo de datos en ':core' con JUnit/Mockito.
- **Pruebas de Red:** Verificar manejo de excepciones de Timeout y desconexión en ':app-sender' y ':app-receiver'.
- **Ejecución del Oráculo:** Todo cambio debe validar la compilación y tests ejecutando:
  .\gradlew.bat clean build

---

## 3. BUENAS PRÁCTICAS Y ESTÁNDAR DE CÓDIGO
- **Límite de Tamaño:** Máximo 300 líneas por archivo. Separar estados, modelos y composables en archivos independientes.
- **Diseño de Código:** Aplicar 'Fail Fast' (evitar 'if-else' anidados). Sin 'Magic Numbers' o puertos de red 'hardcoded'.
- **Comentarios:** Solo explicar el 'POR QUÉ' de un parche o limitación técnica del hardware. Prohibido narrar lo que hace el código.
- **Catálogo de Versiones:** Declarar dependencias únicamente en 'gradle/libs.versions.toml'.

---

## 4. CONTROL DE VERSIONES (GIT)
- **Mensajes de Commit:** Usar Convención Estándar: 'feat(modulo): descripción', 'fix(modulo): descripción'.
- **Ámbito Acotado:** Cada tarea debe modificar únicamente los archivos necesarios sin refactorizaciones ajenas no solicitadas.

---

## 5. DOCUMENTACIÓN VIVA Y CIERRE
- Mantener 'doc/PROJECT_DASHBOARD.md' actualizado al finalizar cada sesión.
- Si se modifica la estructura del protocolo de red o streaming, registrar el cambio en 'doc/adr/ADR-xxx.md'.
