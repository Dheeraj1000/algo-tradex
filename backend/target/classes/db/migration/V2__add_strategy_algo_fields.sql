-- =============================================
-- V2: Add algo execution fields to strategies
-- =============================================

-- Add columns for algo execution engine
ALTER TABLE strategies ADD COLUMN IF NOT EXISTS max_exposure DECIMAL(15,2);
ALTER TABLE strategies ADD COLUMN IF NOT EXISTS ai_threshold DOUBLE PRECISION;
ALTER TABLE strategies ADD COLUMN IF NOT EXISTS is_paper_trading BOOLEAN NOT NULL DEFAULT TRUE;
