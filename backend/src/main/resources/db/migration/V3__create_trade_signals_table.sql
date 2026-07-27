-- =============================================
-- V3: Create trade_signals table properly
-- =============================================

-- Drop the auto-generated table if it exists (Hibernate ddl-auto may have created it with wrong columns)
DROP TABLE IF EXISTS trade_signals CASCADE;

CREATE TABLE trade_signals (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    strategy_id         UUID NOT NULL REFERENCES strategies(id) ON DELETE CASCADE,
    instrument_id       UUID REFERENCES instruments(id),
    signal_type         ai_prediction NOT NULL,
    ai_confidence_score DOUBLE PRECISION,
    execution_status    order_status NOT NULL DEFAULT 'PENDING',
    rejection_reason    TEXT,
    timestamp           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_trade_signals_strategy ON trade_signals(strategy_id);
CREATE INDEX idx_trade_signals_timestamp ON trade_signals(timestamp);
