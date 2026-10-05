# ADR-001: Adopción del Comando Oráculo Unificado y Tipado Estricto con Zod

- **Fecha:** 2026-09-12
- **Estado:** Aprobado
- **Decisores:** Arquitectura de Ecosistema

---

## 1. Contexto y Problema
Al delegar tareas de desarrollo a múltiples agentes de Inteligencia Artificial (o desarrolladores externos), se producían inconsistencias frecuentes: código sin compilar, tipos `any` en capas críticas y omisión de pruebas unitarias. Requerir comprobaciones manuales generaba fricción y entregas fallidas.

---

## 2. Decisión Adoptada
Adoptar como estándar inquebrantable el "Comando Oráculo" (`pnpm verify`) en todos los repositorios y la validación obligatoria en frontera mediante esquemas Zod.
Ningún cambio es aceptado si `pnpm verify` (lint + typecheck + test) no concluye con código de salida 0.

---

## 3. Alternativas Descartadas

| Alternativa | Razón de Descarte |
|---|---|
| Revisión manual aislada | Propensa a omisiones humanas y alucinaciones en modelos de lenguaje. |
| Validación de tipos solo en runtime | No previene errores de compilación estática antes del despliegue. |

---

## 4. Consecuencias
- **Positivas:** Calidad determinista. El 100% del código integrado está tipado y probado.
- **Negativas / Compromisos:** Mayor rigurosidad requerida antes de hacer commit o aprobar un pull request.
- **Impacto en el Código:** Todo controlador HTTP debe exportar un schema Zod de entrada y las interfaces de dominio deben derivar de dicho schema (`z.infer<typeof ...>`).
