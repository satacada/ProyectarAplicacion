-- ==============================================================================
-- OMNICAST SCREEN MIRRORING & STREAMING - CAJA NEGRA Y TELEMETRÍA (SUPABASE)
-- Archivo: supabase/migrations/01_omnicast_telemetry_logs.sql
-- Motor: PostgreSQL 15+ (Supabase)
-- ==============================================================================

-- 1. Crear tabla de logs y telemetría en tiempo real
CREATE TABLE IF NOT EXISTS public.omnicast_system_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role TEXT NOT NULL,                     -- 'tv' (Receptor) o 'phone' (Emisor)
    app_version TEXT NOT NULL DEFAULT 'v3.32.28',
    device_model TEXT NOT NULL,             -- Ej: 'Xiaomi Mi Box S', 'Samsung SM-A546E'
    android_version TEXT,                   -- Ej: 'Android 14 (API 34)'
    level TEXT NOT NULL DEFAULT 'info',     -- 'info', 'warn', 'error'
    event TEXT NOT NULL,                    -- 'app_start', 'webrtc_failed', 'exoplayer_error', etc.
    message TEXT NOT NULL,
    details JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE public.omnicast_system_logs IS 'Caja negra y telemetría remota para OmniCast TV y Celular.';

-- 2. Índices optimizados para consulta y depuración
CREATE INDEX IF NOT EXISTS idx_omnicast_logs_created 
ON public.omnicast_system_logs (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_omnicast_logs_level_created 
ON public.omnicast_system_logs (level, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_omnicast_logs_role_created 
ON public.omnicast_system_logs (role, created_at DESC);

-- 3. Habilitar Seguridad de Nivel de Fila (RLS)
ALTER TABLE public.omnicast_system_logs ENABLE ROW LEVEL SECURITY;

-- Limpieza segura de políticas previas (idempotente)
DO $$
BEGIN
    DROP POLICY IF EXISTS "Permitir insercion anonima omnicast logs" ON public.omnicast_system_logs;
    DROP POLICY IF EXISTS "Permitir lectura publica omnicast logs" ON public.omnicast_system_logs;
END $$;

-- Permitir inserción anónima desde TV Box y Celulares (con anon key)
CREATE POLICY "Permitir insercion anonima omnicast logs"
ON public.omnicast_system_logs
FOR INSERT
TO anon, authenticated
WITH CHECK (true);

-- Permitir lectura para monitoreo remoto y dashboard
CREATE POLICY "Permitir lectura publica omnicast logs"
ON public.omnicast_system_logs
FOR SELECT
TO anon, authenticated
USING (true);

-- 4. Función de Limpieza Automática (Buffer circular: máx 1000 filas y purga > 3 días)
CREATE OR REPLACE FUNCTION public.fn_cleanup_omnicast_logs()
RETURNS INTEGER AS $$
DECLARE
    v_deleted_count INTEGER := 0;
BEGIN
    -- Regla 1: Borrar registros con más de 3 días de antigüedad
    DELETE FROM public.omnicast_system_logs
    WHERE created_at < NOW() - INTERVAL '3 days';
    
    GET DIAGNOSTICS v_deleted_count = ROW_COUNT;

    -- Regla 2: Mantener buffer circular estricto (máximo 1000 registros más recientes)
    DELETE FROM public.omnicast_system_logs
    WHERE id NOT IN (
        SELECT id FROM public.omnicast_system_logs
        ORDER BY created_at DESC
        LIMIT 1000
    );

    RETURN v_deleted_count;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- 5. Disparador probabilístico de auto-limpieza al insertar (costo computacional nulo)
CREATE OR REPLACE FUNCTION public.tr_trigger_cleanup_omnicast_logs()
RETURNS TRIGGER AS $$
BEGIN
    IF (random() < 0.05) THEN
        PERFORM public.fn_cleanup_omnicast_logs();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tr_cleanup_omnicast_probabilistic ON public.omnicast_system_logs;
CREATE TRIGGER tr_cleanup_omnicast_probabilistic
AFTER INSERT ON public.omnicast_system_logs
FOR EACH STATEMENT
EXECUTE FUNCTION public.tr_trigger_cleanup_omnicast_logs();

-- 6. Función de purga total bajo demanda
CREATE OR REPLACE FUNCTION public.fn_drop_omnicast_logs_completely()
RETURNS VOID AS $$
BEGIN
    DROP TABLE IF EXISTS public.omnicast_system_logs CASCADE;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

NOTIFY pgrst, 'reload schema';
