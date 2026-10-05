# ARQUITECTURA DEL SISTEMA: MAPA MENTAL Y FLUJO DE DATOS
## Proyecto: [NOMBRE_DEL_PROYECTO]

---

## 1. Visión General de Alto Nivel
Este documento plasma la topología, los componentes clave y las fronteras de integración del sistema, sirviendo como plano de referencia para humanos e Inteligencias Artificiales.

---

## 2. Diagrama de Contenedores y Flujo (C4 Model Nivel 2)

```mermaid
graph TD
    Client["Cliente / Usuario Final<br/>(Navegador / App Móvil / Webhook)"]
    
    subgraph Plataforma ["Plataforma del Proyecto"]
        Gateway["API Gateway / Servidor HTTP<br/>(Fastify / Express / Next.js)"]
        Domain["Capa de Dominio & Reglas de Negocio<br/>(Lógica Pura sin dependencias)"]
        Infra["Capa de Infraestructura<br/>(Repositorios, Adaptadores, Clientes)"]
    end
    
    subgraph ServiciosExternos ["Servicios Externos y Almacenamiento"]
        DB[(Base de Datos Principal<br/>PostgreSQL / Supabase)]
        Cache[(Caché / Colas<br/>Redis / BullMQ)]
        AIProvider["Proveedor de Inteligencia Artificial<br/>(Gemini / OpenAI / Claude)"]
        ExternalAPI["APIs Terceros / Pasarelas de Pago<br/>(Stripe, Meta, etc.)"]
    end
    
    Client -->|HTTPS / WSS| Gateway
    Gateway -->|Valida Schemas con Zod| Domain
    Domain -->|Invoca Contratos| Infra
    Infra -->|TCP / SQL| DB
    Infra -->|Comandos / PubSub| Cache
    Infra -->|SDK HTTPS| AIProvider
    Infra -->|REST Webhooks| ExternalAPI
```

---

## 3. Catálogo de Puertos y Servicios Locales

| Componente / Servicio | Puerto Local | Protocolo | Endpoint de Salud |
|---|---|---|---|
| API Backend Principal | `3000` | HTTP/REST | `/health` |
| Frontend Web | `5173` o `3001` | HTTP | `/` |
| Base de Datos Local | `5432` | TCP/PostgreSQL | Ping nativo |
| Redis Cache | `6379` | TCP/Redis | Ping nativo |

---

## 4. Fronteras de Validación y Manejo de Errores
1. **Entrada de Datos:** Toda solicitud HTTP pasa por middleware de validación antes de alcanzar los controladores. Si la validación falla, se retorna error estándar `400 Bad Request` con detalles estructurados.
2. **Capa de Dominio:** Ningún modelo de base de datos se expone directamente a la interfaz pública (DTOs obligatorios).
3. **Manejo Centralizado de Excepciones:** Errores no controlados son interceptados, registrados con identificador de traza (`traceId`) y devueltos como `500 Internal Server Error` sin filtrar detalles sensibles.
