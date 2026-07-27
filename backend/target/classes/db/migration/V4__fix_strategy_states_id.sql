-- =============================================
-- V4: Fix strategy_states ID mapping
-- =============================================

-- Drop the auto-generated table so Hibernate recreates it properly with the new UUID id column
DROP TABLE IF EXISTS strategy_states CASCADE;
