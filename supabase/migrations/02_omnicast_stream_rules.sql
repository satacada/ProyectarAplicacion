-- ==============================================================================
-- OMNICAST STREAM RULES & CROWDSOURCED INTELLIGENCE (SUPABASE)
-- Archivo: supabase/migrations/02_omnicast_stream_rules.sql
-- Motor: PostgreSQL 15+ (Supabase)
-- ==============================================================================

-- 1. Tabla dedicada para perfiles de servidores y reglas crowdsourced
CREATE TABLE IF NOT EXISTS public.omnicast_stream_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    host TEXT NOT NULL UNIQUE,
    branch TEXT NOT NULL DEFAULT 'VOD_HLS_MP4',
    bypass_proxy BOOLEAN NOT NULL DEFAULT true,
    headers JSONB DEFAULT '{}'::jsonb,
    success_count INTEGER NOT NULL DEFAULT 1,
    last_verified_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE public.omnicast_stream_rules IS 'Perfiles y reglas de streaming crowdsourced para reproducción directa sin errores 403.';

-- 2. Índices optimizados para consulta y ordenamiento
CREATE INDEX IF NOT EXISTS idx_omnicast_stream_rules_host 
ON public.omnicast_stream_rules (host);

CREATE INDEX IF NOT EXISTS idx_omnicast_stream_rules_updated 
ON public.omnicast_stream_rules (last_verified_at DESC);

-- 3. Habilitar Seguridad de Nivel de Fila (RLS)
ALTER TABLE public.omnicast_stream_rules ENABLE ROW LEVEL SECURITY;

-- Limpieza segura de políticas previas (idempotente)
DO $$
BEGIN
    DROP POLICY IF EXISTS "Permitir insercion anonima stream rules" ON public.omnicast_stream_rules;
    DROP POLICY IF EXISTS "Permitir lectura publica stream rules" ON public.omnicast_stream_rules;
    DROP POLICY IF EXISTS "Permitir actualizacion anonima stream rules" ON public.omnicast_stream_rules;
END $$;

-- Permitir inserción anónima desde clientes OmniCast
CREATE POLICY "Permitir insercion anonima stream rules"
ON public.omnicast_stream_rules
FOR INSERT
TO anon, authenticated
WITH CHECK (true);

-- Permitir lectura pública de reglas
CREATE POLICY "Permitir lectura publica stream rules"
ON public.omnicast_stream_rules
FOR SELECT
TO anon, authenticated
USING (true);

-- Permitir actualización anónima (upsert / actualización de timestamp y contador)
CREATE POLICY "Permitir actualizacion anonima stream rules"
ON public.omnicast_stream_rules
FOR UPDATE
TO anon, authenticated
USING (true)
WITH CHECK (true);

NOTIFY pgrst, 'reload schema';
