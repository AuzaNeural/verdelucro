-- ==============================================================================
-- AUZA NEURAL - GIGU COPILOTO PRO
-- Schema de Banco de Dados Oficial (Supabase / PostgreSQL)
-- ==============================================================================

-- 1. Habilitar UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. Tabela de Perfis de Motoristas (Auza Neural)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name TEXT NOT NULL,
    email TEXT UNIQUE NOT NULL,
    phone TEXT,
    city TEXT DEFAULT 'São Paulo',
    vehicle_model TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Configurações de Veículo e Combustíveis
CREATE TABLE IF NOT EXISTS public.vehicle_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    active_fuel TEXT CHECK (active_fuel IN ('GASOLINA', 'ETANOL', 'GNV')) DEFAULT 'GASOLINA',
    gasoline_price NUMERIC(6,2) DEFAULT 5.89,
    gasoline_kml NUMERIC(5,2) DEFAULT 10.5,
    ethanol_price NUMERIC(6,2) DEFAULT 3.99,
    ethanol_kml NUMERIC(5,2) DEFAULT 7.5,
    gnv_price NUMERIC(6,2) DEFAULT 4.49,
    gnv_kml NUMERIC(5,2) DEFAULT 13.0,
    maintenance_per_km NUMERIC(5,2) DEFAULT 0.20,
    depreciation_per_km NUMERIC(5,2) DEFAULT 0.15,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id)
);

-- 4. Metas e Régua do Semáforo (Ruim / Médio / Bom)
CREATE TABLE IF NOT EXISTS public.driver_targets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    bad_rate_km NUMERIC(5,2) DEFAULT 1.85,
    good_rate_km NUMERIC(5,2) DEFAULT 2.35,
    bad_rate_hour NUMERIC(6,2) DEFAULT 35.00,
    good_rate_hour NUMERIC(6,2) DEFAULT 45.00,
    monthly_gross_target NUMERIC(10,2) DEFAULT 10833.00,
    monthly_cost_target NUMERIC(10,2) DEFAULT 2879.00,
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id)
);

-- 5. Histórico de Corridas Analisadas
CREATE TABLE IF NOT EXISTS public.rides (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    platform TEXT CHECK (platform IN ('UBER', 'NOVENTA_E_NOVE', 'IFOOD', 'INDRIVE', 'KEETA', 'URBANO_NORTE', 'UNKNOWN')) NOT NULL,
    fare NUMERIC(8,2) NOT NULL,
    total_km NUMERIC(6,2) NOT NULL,
    time_minutes INT NOT NULL,
    rate_km NUMERIC(6,2) NOT NULL,
    rate_hour NUMERIC(6,2) NOT NULL,
    total_cost NUMERIC(8,2) NOT NULL,
    net_profit NUMERIC(8,2) NOT NULL,
    profit_percentage NUMERIC(5,2) NOT NULL,
    color TEXT CHECK (color IN ('GREEN', 'YELLOW', 'RED', 'PURPLE_RISK')) NOT NULL,
    is_risk BOOLEAN DEFAULT FALSE,
    surge_multiplier NUMERIC(4,2),
    pickup_address TEXT,
    destination_address TEXT,
    passenger_rating NUMERIC(3,2),
    accepted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 6. Lista Negra de Áreas de Risco (Colaborativa da Auza Neural)
CREATE TABLE IF NOT EXISTS public.risk_areas (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    city TEXT NOT NULL DEFAULT 'São Paulo',
    neighborhood_name TEXT NOT NULL,
    description TEXT,
    verified BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(city, neighborhood_name)
);

-- Inserção de Áreas de Risco Padrão
INSERT INTO public.risk_areas (city, neighborhood_name, description) VALUES
('São Paulo', 'Heliópolis', 'Região de alto índice de assaltos noturnos'),
('São Paulo', 'Paraisópolis', 'Comunidade com áreas sem sinal de GPS'),
('São Paulo', 'Cracolândia', 'Zona central de alto risco e bloqueios'),
('São Paulo', 'Capão Redondo', 'Acesso restrito em vias secundárias')
ON CONFLICT DO NOTHING;

-- 7. Configuração de Row Level Security (RLS)
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.vehicle_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.driver_targets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.rides ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.risk_areas ENABLE ROW LEVEL SECURITY;

-- Políticas de Acesso
CREATE POLICY "Motorista acessa seu proprio perfil" ON public.profiles
    FOR ALL USING (auth.uid() = id);

CREATE POLICY "Motorista gerencia seu veiculo" ON public.vehicle_settings
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Motorista gerencia suas metas" ON public.driver_targets
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Motorista visualiza suas corridas" ON public.rides
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY "Todos os motoristas leem areas de risco" ON public.risk_areas
    FOR SELECT TO authenticated USING (true);

-- 8. View para Resumo Diário
CREATE OR REPLACE VIEW public.view_daily_summary AS
SELECT 
    user_id,
    DATE(created_at) AS ride_date,
    COUNT(*) AS total_rides,
    COUNT(*) FILTER (WHERE color = 'GREEN') AS green_rides,
    COUNT(*) FILTER (WHERE color = 'YELLOW') AS yellow_rides,
    COUNT(*) FILTER (WHERE color = 'RED') AS red_rides,
    COUNT(*) FILTER (WHERE color = 'PURPLE_RISK') AS risk_rides,
    SUM(fare) AS total_gross_fare,
    SUM(net_profit) AS total_net_profit,
    SUM(total_km) AS total_km_driven,
    SUM(time_minutes) AS total_minutes
FROM public.rides
GROUP BY user_id, DATE(created_at);

