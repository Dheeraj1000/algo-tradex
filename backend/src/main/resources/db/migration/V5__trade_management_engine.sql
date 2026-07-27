-- Add new fields to trade_signals
ALTER TABLE trade_signals ADD COLUMN recovery_probability DOUBLE PRECISION;
ALTER TABLE trade_signals ADD COLUMN exit_reason VARCHAR(255);
ALTER TABLE trade_signals ADD COLUMN ai_explanation VARCHAR(1000);
ALTER TABLE trade_signals ADD COLUMN current_stop_loss DOUBLE PRECISION;
ALTER TABLE trade_signals ADD COLUMN max_profit_reached DOUBLE PRECISION;

-- Create trade_management_logs table
CREATE TABLE trade_management_logs (
    id UUID PRIMARY KEY,
    trade_signal_id UUID NOT NULL,
    action_taken VARCHAR(50) NOT NULL,
    recovery_probability DOUBLE PRECISION,
    explanation VARCHAR(1000),
    spot_price DOUBLE PRECISION,
    option_price DOUBLE PRECISION,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (trade_signal_id) REFERENCES trade_signals(id)
);

-- Create position_states table
CREATE TABLE position_states (
    id UUID PRIMARY KEY,
    trade_signal_id UUID NOT NULL UNIQUE,
    current_quantity INTEGER NOT NULL,
    partial_exits_count INTEGER DEFAULT 0,
    reentry_count INTEGER DEFAULT 0,
    is_in_recovery BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE,
    FOREIGN KEY (trade_signal_id) REFERENCES trade_signals(id)
);
