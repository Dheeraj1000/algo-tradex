CREATE TABLE strategy_states (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    strategy_id UUID NOT NULL UNIQUE REFERENCES strategies(id) ON DELETE CASCADE,
    daily_realized_pnl DECIMAL(19, 4),
    daily_trade_count INTEGER NOT NULL DEFAULT 0,
    last_evaluation_time TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
