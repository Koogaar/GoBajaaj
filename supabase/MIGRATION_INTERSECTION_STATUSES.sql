-- ==========================================================
-- GOBAJAAJ: UPDATE INTERSECTION AND TRAFFIC REPORT CHECK CONSTRAINTS
-- Xalka saxda ah: Ogolow dhammaan magacyada xaaladaha saxmadda
-- (Furan, Normal, Jaam, Xeran, Xiran, Dhexdhexaad, Mashquul, Saxmad badan)
-- ==========================================================

-- 1. Intersections constraint
ALTER TABLE public.intersections DROP CONSTRAINT IF EXISTS intersections_current_status_check;
ALTER TABLE public.intersections ADD CONSTRAINT intersections_current_status_check
    CHECK (current_status IN ('Furan', 'Normal', 'Jaam', 'Xeran', 'Xiran', 'Dhexdhexaad', 'Mashquul', 'Saxmad badan'));

-- 2. Traffic reports constraint
ALTER TABLE public.traffic_reports DROP CONSTRAINT IF EXISTS traffic_reports_status_check;
ALTER TABLE public.traffic_reports ADD CONSTRAINT traffic_reports_status_check
    CHECK (status IN ('Furan', 'Normal', 'Jaam', 'Xeran', 'Xiran', 'Dhexdhexaad', 'Mashquul', 'Saxmad badan'));

-- 3. Fasaxa RLS (Update and Insert)
ALTER TABLE public.intersections DISABLE ROW LEVEL SECURITY;
GRANT ALL ON public.intersections TO anon, authenticated, service_role;
GRANT ALL ON public.traffic_reports TO anon, authenticated, service_role;

