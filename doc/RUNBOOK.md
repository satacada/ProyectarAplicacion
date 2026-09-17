# RUNBOOK OPERATIVO: MANUAL DE RESCATE (3 AM) Y ARRANQUE EN FRÍO
## Proyecto: [NOMBRE_DEL_PROYECTO]

> **Objetivo:** Permitir que cualquier desarrollador humano o agente de IA pueda recuperar el servicio ante caídas críticas o levantar el proyecto desde cero en una máquina limpia sin necesidad de consultar a nadie.

---

## 1. Arranque en Frío (Desde Cero en Máquina Limpia)

### Prerrequisitos
- Node.js versión `>= 20.x`
- Gestor de paquetes: `pnpm` (versión `>= 9.x`)
- Docker Desktop o servicio de base de datos accesible

### Pasos de Inicialización
```bash
# 1. Clonar o posicionarse en el repositorio
cd [directorio-del-proyecto]

# 2. Configurar variables de entorno iniciales
cp .env.example .env

# 3. Instalar dependencias con candado estricto
pnpm install --frozen-lockfile

# 4. Ejecutar migraciones o semillas de base de datos
pnpm db:migrate

# 5. Ejecutar la prueba oráculo para certificar el entorno
pnpm verify

# 6. Iniciar en modo desarrollo
pnpm dev
```

---

## 2. Diccionario de Variables de Entorno (.env)

| Variable | Requerida | Ejemplo / Formato | Descripción y Propósito |
|---|---|---|---|
| `NODE_ENV` | Sí | `development` / `production` | Modo de ejecución del runtime. |
| `PORT` | Sí | `3000` | Puerto en el que escucha el servidor HTTP. |
| `DATABASE_URL` | Sí | `postgresql://user:pass@host:5432/db` | Conexión con pooling a PostgreSQL. |
| `API_SECRET_KEY` | Sí | `cadena_alfanumerica_segura` | Clave criptográfica para firma de tokens. |
| `AI_API_KEY` | Opcional | `sk-proj-...` | Credencial para el proveedor de IA. |

---

## 3. Matriz de Incidentes Comunes y Procedimientos de Rescate (3 AM)

### Incidente 1: El servidor no inicia o se cuelga al arrancar
- **Síntoma:** Error de tipo `EADDRINUSE: address already in use :::3000`.
- **Causa Raíz:** Un proceso zombi quedó ejecutándose en el puerto asignado.
- **Solución Rápida (Windows):**
  ```powershell
  Get-Process -Id (Get-NetTCPConnection -LocalPort 3000).OwningProcess | Stop-Process -Force
  ```
- **Solución Rápida (Linux/macOS):**
  ```bash
  kill -9 $(lsof -t -i:3000)
  ```

### Incidente 2: Falla de conexión a base de datos
- **Síntoma:** `Connection refused to localhost:5432` o `Error: P1001: Can't reach database server`.
- **Causa Raíz:** Contenedor detenido o credenciales desalineadas en `.env`.
- **Procedimiento de Validación:**
  1. Verificar si el servicio local o remoto responde: `pnpm db:status`
  2. Comprobar que `DATABASE_URL` apunte al host correcto y no a una IP desactualizada.
  3. Probar conexión básica con cliente CLI antes de reiniciar la aplicación.

### Incidente 3: Incoherencia en Tipos / Cache Corrupta
- **Síntoma:** El compilador arroja errores inexplicables en librerías o módulos tras una actualización.
- **Solución Radical:**
  ```bash
  # Limpiar artefactos y recrear dependencias
  pnpm clean || rm -rf dist .turbo .next node_modules
  pnpm install
  pnpm build
  ```

---

## 4. Verificación Post-Recuperación
Tras aplicar cualquier remedio en producción o desarrollo:
1. Ejecutar: `curl -s http://localhost:3000/health` (debe devolver status `healthy`).
2. Comprobar logs en vivo en búsqueda de excepciones no capturadas.
3. Notificar al equipo registrando la causa y la solución en un informe de incidente.
