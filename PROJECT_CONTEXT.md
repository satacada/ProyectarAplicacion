# CONTEXTO DEL PROYECTO Y REGLAS DE ORO - [NOMBRE_DEL_PROYECTO]

> **Instrucción para cualquier IA o Desarrollador:**
> Este documento es la fuente única de verdad para el contexto, las reglas inquebrantables y los comandos de este repositorio.
> Todo cambio debe adherirse estrictamente a las directrices aquí estipuladas.

---

## 1. Identidad y Misión del Proyecto
- **Propósito:** [Resumen de 2 líneas: qué problema resuelve este software, a quién está dirigido y cuál es su valor principal].
- **Tipo de Aplicación:** [Ej: Monorepo SaaS, API Backend REST/GraphQL, Frontend Web SPA, Microservicio].
- **Stack Tecnológico Principal:** [Ej: TypeScript / Node.js / Fastify / PostgreSQL / React / Tailwind].
- **Gestor de Paquetes Obligatorio:** [Ej: pnpm (PROHIBIDO usar npm o yarn)].

---

## 2. Reglas Inquebrantables (Golden Rules)
1. **Tipado Estricto:** Prohibido el uso de `any`. Toda entidad o payload debe contar con interfaces o tipos inferidos de schemas.
2. **Validación en Frontera:** Toda entrada externa (HTTP params, body, headers, webhooks, variables de entorno) DEBE validarse con esquemas estrictos (Zod, Valibot, etc.).
3. **Cero Mocks en Producción:** Los mocks solo son admisibles dentro de suites de pruebas unitarias aisladas (`*.spec.ts` o `*.test.ts`).
4. **Seguridad y RLS:** Prohibido alterar políticas de seguridad, Row-Level Security o cadenas de conexión sin aprobación explícita.
5. **Cero Asunciones:** Si una regla de negocio o cálculo crítico presenta ambigüedad, la IA debe detenerse y preguntar antes de codificar.
6. **Integridad de Código:** Prohibido dejar archivos a medio escribir, eliminar código preexistente sin justificación o añadir dependencias externas sin documentar en un ADR.

---

## 3. Comandos Clave del Repositorio
- **Instalar Dependencias:** `pnpm install`
- **Levantar Entorno Local:** `pnpm dev`
- **Ejecutar Suite de Pruebas:** `pnpm test`
- **Verificar Compilación y Tipos:** `pnpm build`
- **COMANDO ORÁCULO DE CALIDAD:** `pnpm verify`
  *(Requisito sine qua non: debe finalizar con código de salida 0 antes de entregar cualquier cambio).*

---

## 4. Mapa Arquitectónico
- `apps/` o `src/`: Código fuente de las aplicaciones y servicios.
- `packages/`: Librerías internas compartidas (si es monorepo).
- `doc/`: Documentación viva y obligatoria:
  - `doc/RUNBOOK.md`: Manual de rescate ante incidentes y arranque en frío.
  - `doc/ARCHITECTURE.md`: Diagramas C4 y flujo de componentes.
  - `doc/adr/`: Registro formal de decisiones técnicas y de arquitectura.
  - `doc/openapi.json`: Contrato de interfaz de red (si aplica).

---

## 5. Instrucción de Entrega Obligatoria
Antes de dar por concluida cualquier intervención:
1. Ejecuta el comando oráculo (`pnpm verify` / `npm run verify`).
2. Si se produce un error de tipos, linter o fallo en pruebas, corrígelo de inmediato.
3. No entregues código roto, archivos incompletos ni commits sin verificar.

## 4. TelemetrÃ­a de TV (Sin ADB)
* :app-receiver debe enviar sus logs/excepciones con timestamp vÃ­a red local a :app-sender o a la PC.
* Para depurar la TV, la IA debe leer el archivo de logs/eventos recibidos en lugar de ejecutar 'adb logcat'.
