ALTER TABLE trade_signals ALTER COLUMN signal_type TYPE VARCHAR(50) USING signal_type::text;
