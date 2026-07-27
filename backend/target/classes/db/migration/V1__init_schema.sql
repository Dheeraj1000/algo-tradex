-- =============================================
-- AlgoTradeX Database Schema v1
-- PostgreSQL 16
-- =============================================

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================
-- ENUM TYPES
-- =============================================
CREATE TYPE user_role AS ENUM ('ADMIN', 'TRADER', 'VIEWER');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED', 'PENDING_VERIFICATION');
CREATE TYPE broker_type AS ENUM ('ANGEL_ONE', 'UPSTOX', 'SHOONYA', 'DHAN', 'ZERODHA', 'GROWW', 'MOCK');
CREATE TYPE broker_status AS ENUM ('CONNECTED', 'DISCONNECTED', 'TOKEN_EXPIRED', 'ERROR');
CREATE TYPE order_side AS ENUM ('BUY', 'SELL');
CREATE TYPE order_type AS ENUM ('MARKET', 'LIMIT', 'SL', 'SL_MARKET');
CREATE TYPE order_status AS ENUM ('PENDING', 'OPEN', 'COMPLETE', 'CANCELLED', 'REJECTED', 'TRIGGER_PENDING');
CREATE TYPE position_status AS ENUM ('OPEN', 'CLOSED', 'PARTIALLY_CLOSED');
CREATE TYPE product_type AS ENUM ('INTRADAY', 'DELIVERY', 'CARRYFORWARD');
CREATE TYPE instrument_type AS ENUM ('EQ', 'FUT', 'CE', 'PE', 'INDEX');
CREATE TYPE exchange_type AS ENUM ('NSE', 'BSE', 'NFO', 'BFO', 'MCX', 'CDS');
CREATE TYPE strategy_status AS ENUM ('DRAFT', 'ACTIVE', 'PAUSED', 'STOPPED', 'ARCHIVED');
CREATE TYPE strategy_type AS ENUM ('INTRADAY', 'POSITIONAL', 'SCALPING', 'SWING');
CREATE TYPE backtest_status AS ENUM ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED');
CREATE TYPE trading_mode AS ENUM ('PAPER', 'LIVE');
CREATE TYPE notification_channel AS ENUM ('TELEGRAM', 'EMAIL', 'PUSH');
CREATE TYPE notification_type AS ENUM ('TRADE_EXECUTED', 'TRADE_CLOSED', 'SL_HIT', 'TARGET_HIT', 'DAILY_REPORT', 'SYSTEM_ALERT', 'AI_SIGNAL');
CREATE TYPE report_period AS ENUM ('DAILY', 'WEEKLY', 'MONTHLY', 'YEARLY');
CREATE TYPE ai_model_type AS ENUM ('LSTM', 'TRANSFORMER', 'XGBOOST', 'RANDOM_FOREST', 'CATBOOST', 'ENSEMBLE');
CREATE TYPE ai_prediction AS ENUM ('BUY', 'SELL', 'NO_TRADE');
CREATE TYPE ai_job_status AS ENUM ('QUEUED', 'TRAINING', 'COMPLETED', 'FAILED');
CREATE TYPE timeframe AS ENUM ('1m', '3m', '5m', '15m', '30m', '1H', '1D');

-- =============================================
-- USERS & AUTH
-- =============================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    role            user_role NOT NULL DEFAULT 'TRADER',
    status          user_status NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    two_fa_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    two_fa_secret   VARCHAR(255),
    avatar_url      VARCHAR(500),
    last_login_at   TIMESTAMP WITH TIME ZONE,
    last_login_ip   VARCHAR(45),
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_email ON users(email) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_role ON users(role) WHERE deleted_at IS NULL;
CREATE INDEX idx_users_status ON users(status) WHERE deleted_at IS NULL;

CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(500) NOT NULL UNIQUE,
    device_info VARCHAR(255),
    ip_address  VARCHAR(45),
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked     BOOLEAN NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX idx_refresh_tokens_expires ON refresh_tokens(expires_at) WHERE revoked = FALSE;

CREATE TABLE email_verification_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE password_reset_tokens (
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- =============================================
-- AUDIT LOG
-- =============================================
CREATE TABLE audit_log (
    id          BIGSERIAL PRIMARY KEY,
    user_id     UUID REFERENCES users(id),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   VARCHAR(255),
    old_value   JSONB,
    new_value   JSONB,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    metadata    JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_log_user ON audit_log(user_id);
CREATE INDEX idx_audit_log_action ON audit_log(action);
CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_created ON audit_log(created_at);

-- =============================================
-- INSTRUMENTS & MARKET DATA
-- =============================================
CREATE TABLE instruments (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    symbol          VARCHAR(50) NOT NULL,
    trading_symbol  VARCHAR(100) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    exchange        exchange_type NOT NULL,
    instrument_type instrument_type NOT NULL,
    segment         VARCHAR(20),
    lot_size        INT NOT NULL DEFAULT 1,
    tick_size       DECIMAL(10,4) NOT NULL DEFAULT 0.05,
    expiry          DATE,
    strike          DECIMAL(12,2),
    option_type     VARCHAR(2),
    isin            VARCHAR(20),
    token           VARCHAR(50),
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_instruments_trading_symbol_exchange ON instruments(trading_symbol, exchange);
CREATE INDEX idx_instruments_symbol ON instruments(symbol);
CREATE INDEX idx_instruments_type ON instruments(instrument_type);
CREATE INDEX idx_instruments_expiry ON instruments(expiry) WHERE expiry IS NOT NULL;

CREATE TABLE market_data_candles (
    id              BIGSERIAL PRIMARY KEY,
    instrument_id   UUID NOT NULL REFERENCES instruments(id),
    timeframe       timeframe NOT NULL,
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL,
    open            DECIMAL(12,2) NOT NULL,
    high            DECIMAL(12,2) NOT NULL,
    low             DECIMAL(12,2) NOT NULL,
    close           DECIMAL(12,2) NOT NULL,
    volume          BIGINT NOT NULL DEFAULT 0,
    oi              BIGINT DEFAULT 0,
    vwap            DECIMAL(12,2),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_market_data_unique ON market_data_candles(instrument_id, timeframe, timestamp);
CREATE INDEX idx_market_data_instrument_time ON market_data_candles(instrument_id, timestamp DESC);

CREATE TABLE option_chain_snapshot (
    id              BIGSERIAL PRIMARY KEY,
    underlying_id   UUID NOT NULL REFERENCES instruments(id),
    timestamp       TIMESTAMP WITH TIME ZONE NOT NULL,
    expiry          DATE NOT NULL,
    strike          DECIMAL(12,2) NOT NULL,
    -- Call side
    call_oi         BIGINT DEFAULT 0,
    call_change_oi  BIGINT DEFAULT 0,
    call_volume     BIGINT DEFAULT 0,
    call_iv         DECIMAL(8,4),
    call_ltp        DECIMAL(12,2),
    call_bid        DECIMAL(12,2),
    call_ask        DECIMAL(12,2),
    call_delta      DECIMAL(8,6),
    call_gamma      DECIMAL(8,6),
    call_theta      DECIMAL(8,6),
    call_vega       DECIMAL(8,6),
    -- Put side
    put_oi          BIGINT DEFAULT 0,
    put_change_oi   BIGINT DEFAULT 0,
    put_volume      BIGINT DEFAULT 0,
    put_iv          DECIMAL(8,4),
    put_ltp         DECIMAL(12,2),
    put_bid         DECIMAL(12,2),
    put_ask         DECIMAL(12,2),
    put_delta       DECIMAL(8,6),
    put_gamma       DECIMAL(8,6),
    put_theta       DECIMAL(8,6),
    put_vega        DECIMAL(8,6),
    -- Derived
    pcr             DECIMAL(8,4),
    max_pain        DECIMAL(12,2),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_option_chain_underlying ON option_chain_snapshot(underlying_id, expiry, timestamp DESC);
CREATE INDEX idx_option_chain_strike ON option_chain_snapshot(strike, expiry);

-- =============================================
-- BROKER ACCOUNTS
-- =============================================
CREATE TABLE broker_accounts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    broker_type     broker_type NOT NULL,
    display_name    VARCHAR(100),
    client_id       VARCHAR(100),
    api_key_enc     TEXT,
    api_secret_enc  TEXT,
    access_token    TEXT,
    refresh_token   TEXT,
    token_expiry    TIMESTAMP WITH TIME ZONE,
    status          broker_status NOT NULL DEFAULT 'DISCONNECTED',
    last_connected  TIMESTAMP WITH TIME ZONE,
    metadata        JSONB,
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_broker_accounts_user ON broker_accounts(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_broker_accounts_type ON broker_accounts(broker_type) WHERE deleted_at IS NULL;

-- =============================================
-- STRATEGIES
-- =============================================
CREATE TABLE strategies (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    strategy_type   strategy_type NOT NULL DEFAULT 'INTRADAY',
    status          strategy_status NOT NULL DEFAULT 'DRAFT',
    config          JSONB NOT NULL DEFAULT '{}',
    -- Entry/Exit rules stored as structured JSON
    entry_rules     JSONB NOT NULL DEFAULT '[]',
    exit_rules      JSONB NOT NULL DEFAULT '[]',
    -- Risk management
    stop_loss_type  VARCHAR(20),
    stop_loss_value DECIMAL(10,2),
    trailing_sl     BOOLEAN NOT NULL DEFAULT FALSE,
    trailing_sl_value DECIMAL(10,2),
    target_value    DECIMAL(10,2),
    max_trades_per_day INT DEFAULT 10,
    max_daily_loss  DECIMAL(10,2),
    risk_per_trade  DECIMAL(5,2) DEFAULT 1.0,
    position_size   INT DEFAULT 1,
    -- Scheduling
    trade_start_time TIME,
    trade_end_time  TIME,
    allowed_days    VARCHAR(50) DEFAULT 'MON,TUE,WED,THU,FRI',
    -- Instruments
    instruments     JSONB NOT NULL DEFAULT '[]',
    -- Versioning
    version         INT NOT NULL DEFAULT 1,
    is_public       BOOLEAN NOT NULL DEFAULT FALSE,
    tags            TEXT[],
    total_pnl       DECIMAL(15,2) DEFAULT 0,
    total_trades    INT DEFAULT 0,
    win_rate        DECIMAL(5,2) DEFAULT 0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_strategies_user ON strategies(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_strategies_status ON strategies(status) WHERE deleted_at IS NULL;

CREATE TABLE strategy_versions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    strategy_id     UUID NOT NULL REFERENCES strategies(id) ON DELETE CASCADE,
    version         INT NOT NULL,
    config          JSONB NOT NULL,
    entry_rules     JSONB NOT NULL,
    exit_rules      JSONB NOT NULL,
    changelog       TEXT,
    created_by      UUID REFERENCES users(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_strategy_versions ON strategy_versions(strategy_id, version);

-- =============================================
-- ORDERS & TRADES
-- =============================================
CREATE TABLE orders (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id),
    strategy_id         UUID REFERENCES strategies(id),
    broker_account_id   UUID REFERENCES broker_accounts(id),
    instrument_id       UUID REFERENCES instruments(id),
    trading_mode        trading_mode NOT NULL DEFAULT 'PAPER',
    -- Order details
    side                order_side NOT NULL,
    order_type          order_type NOT NULL,
    product_type        product_type NOT NULL DEFAULT 'INTRADAY',
    quantity            INT NOT NULL,
    price               DECIMAL(12,2),
    trigger_price       DECIMAL(12,2),
    disclosed_qty       INT DEFAULT 0,
    -- Status
    status              order_status NOT NULL DEFAULT 'PENDING',
    broker_order_id     VARCHAR(100),
    filled_qty          INT NOT NULL DEFAULT 0,
    avg_fill_price      DECIMAL(12,2),
    -- Metadata
    tag                 VARCHAR(50),
    reason              TEXT,
    error_message       TEXT,
    parent_order_id     UUID REFERENCES orders(id),
    -- Timestamps
    placed_at           TIMESTAMP WITH TIME ZONE,
    filled_at           TIMESTAMP WITH TIME ZONE,
    cancelled_at        TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_strategy ON orders(strategy_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_broker_order ON orders(broker_order_id);
CREATE INDEX idx_orders_created ON orders(created_at DESC);

CREATE TABLE trades (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id),
    strategy_id     UUID REFERENCES strategies(id),
    order_id        UUID REFERENCES orders(id),
    instrument_id   UUID REFERENCES instruments(id),
    trading_mode    trading_mode NOT NULL DEFAULT 'PAPER',
    side            order_side NOT NULL,
    quantity        INT NOT NULL,
    price           DECIMAL(12,2) NOT NULL,
    brokerage       DECIMAL(10,2) DEFAULT 0,
    taxes           DECIMAL(10,2) DEFAULT 0,
    pnl             DECIMAL(15,2),
    pnl_percent     DECIMAL(8,2),
    -- Entry/Exit linkage
    is_entry        BOOLEAN NOT NULL DEFAULT TRUE,
    linked_trade_id UUID REFERENCES trades(id),
    -- Metadata
    notes           TEXT,
    tags            TEXT[],
    executed_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_trades_user ON trades(user_id);
CREATE INDEX idx_trades_strategy ON trades(strategy_id);
CREATE INDEX idx_trades_instrument ON trades(instrument_id);
CREATE INDEX idx_trades_executed ON trades(executed_at DESC);
CREATE INDEX idx_trades_mode ON trades(trading_mode);

CREATE TABLE positions (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL REFERENCES users(id),
    strategy_id         UUID REFERENCES strategies(id),
    broker_account_id   UUID REFERENCES broker_accounts(id),
    instrument_id       UUID REFERENCES instruments(id),
    trading_mode        trading_mode NOT NULL DEFAULT 'PAPER',
    side                order_side NOT NULL,
    quantity            INT NOT NULL,
    avg_entry_price     DECIMAL(12,2) NOT NULL,
    current_price       DECIMAL(12,2),
    unrealized_pnl      DECIMAL(15,2) DEFAULT 0,
    realized_pnl        DECIMAL(15,2) DEFAULT 0,
    product_type        product_type NOT NULL DEFAULT 'INTRADAY',
    status              position_status NOT NULL DEFAULT 'OPEN',
    stop_loss           DECIMAL(12,2),
    target              DECIMAL(12,2),
    trailing_sl         DECIMAL(12,2),
    opened_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    closed_at           TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_positions_user ON positions(user_id);
CREATE INDEX idx_positions_strategy ON positions(strategy_id);
CREATE INDEX idx_positions_status ON positions(status);
CREATE INDEX idx_positions_mode ON positions(trading_mode);

-- =============================================
-- BACKTESTING
-- =============================================
CREATE TABLE backtests (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id),
    strategy_id     UUID NOT NULL REFERENCES strategies(id),
    name            VARCHAR(200),
    status          backtest_status NOT NULL DEFAULT 'QUEUED',
    -- Configuration
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    initial_capital DECIMAL(15,2) NOT NULL DEFAULT 100000,
    timeframe       timeframe NOT NULL DEFAULT '5m',
    slippage        DECIMAL(5,2) DEFAULT 0.1,
    commission      DECIMAL(5,2) DEFAULT 20,
    instruments     JSONB NOT NULL DEFAULT '[]',
    config          JSONB,
    -- Results
    final_capital   DECIMAL(15,2),
    total_pnl       DECIMAL(15,2),
    total_trades    INT,
    winning_trades  INT,
    losing_trades   INT,
    win_rate        DECIMAL(5,2),
    profit_factor   DECIMAL(8,2),
    sharpe_ratio    DECIMAL(8,4),
    sortino_ratio   DECIMAL(8,4),
    max_drawdown    DECIMAL(8,2),
    max_drawdown_pct DECIMAL(8,2),
    avg_rr_ratio    DECIMAL(8,2),
    avg_holding_time VARCHAR(50),
    equity_curve    JSONB,
    drawdown_curve  JSONB,
    monthly_returns JSONB,
    results_summary JSONB,
    -- Report
    report_file_path VARCHAR(500),
    error_message   TEXT,
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_backtests_user ON backtests(user_id);
CREATE INDEX idx_backtests_strategy ON backtests(strategy_id);
CREATE INDEX idx_backtests_status ON backtests(status);

CREATE TABLE backtest_trades (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    backtest_id     UUID NOT NULL REFERENCES backtests(id) ON DELETE CASCADE,
    instrument_id   UUID REFERENCES instruments(id),
    trade_number    INT NOT NULL,
    side            order_side NOT NULL,
    entry_price     DECIMAL(12,2) NOT NULL,
    exit_price      DECIMAL(12,2),
    quantity        INT NOT NULL,
    pnl             DECIMAL(15,2),
    pnl_percent     DECIMAL(8,2),
    entry_time      TIMESTAMP WITH TIME ZONE NOT NULL,
    exit_time       TIMESTAMP WITH TIME ZONE,
    exit_reason     VARCHAR(50),
    holding_time    VARCHAR(50),
    entry_signal    VARCHAR(100),
    exit_signal     VARCHAR(100)
);

CREATE INDEX idx_backtest_trades_backtest ON backtest_trades(backtest_id);

-- =============================================
-- PAPER TRADING
-- =============================================
CREATE TABLE paper_accounts (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL DEFAULT 'Default Paper Account',
    initial_capital DECIMAL(15,2) NOT NULL DEFAULT 1000000,
    current_capital DECIMAL(15,2) NOT NULL DEFAULT 1000000,
    total_pnl       DECIMAL(15,2) NOT NULL DEFAULT 0,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_paper_accounts_user ON paper_accounts(user_id);

-- =============================================
-- AI MODULE
-- =============================================
CREATE TABLE ai_models (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(200) NOT NULL,
    model_type      ai_model_type NOT NULL,
    version         VARCHAR(20) NOT NULL DEFAULT '1.0',
    description     TEXT,
    -- Performance
    accuracy        DECIMAL(5,2),
    precision_score DECIMAL(5,2),
    recall_score    DECIMAL(5,2),
    f1_score        DECIMAL(5,2),
    -- Configuration
    config          JSONB,
    features        JSONB,
    hyperparameters JSONB,
    -- Storage
    file_path       VARCHAR(500),
    file_size_bytes BIGINT,
    -- Status
    is_active       BOOLEAN NOT NULL DEFAULT FALSE,
    is_production   BOOLEAN NOT NULL DEFAULT FALSE,
    trained_at      TIMESTAMP WITH TIME ZONE,
    training_duration_secs INT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE ai_predictions (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_id        UUID NOT NULL REFERENCES ai_models(id),
    instrument_id   UUID REFERENCES instruments(id),
    prediction      ai_prediction NOT NULL,
    confidence      DECIMAL(5,4) NOT NULL,
    features_snapshot JSONB,
    explanation     TEXT,
    -- Outcome tracking
    actual_outcome  ai_prediction,
    was_correct     BOOLEAN,
    pnl_if_followed DECIMAL(15,2),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_predictions_model ON ai_predictions(model_id);
CREATE INDEX idx_ai_predictions_instrument ON ai_predictions(instrument_id);
CREATE INDEX idx_ai_predictions_created ON ai_predictions(created_at DESC);

CREATE TABLE ai_training_jobs (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    model_id        UUID REFERENCES ai_models(id),
    model_type      ai_model_type NOT NULL,
    status          ai_job_status NOT NULL DEFAULT 'QUEUED',
    config          JSONB,
    metrics         JSONB,
    error_message   TEXT,
    progress        DECIMAL(5,2) DEFAULT 0,
    started_at      TIMESTAMP WITH TIME ZONE,
    completed_at    TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_training_jobs_status ON ai_training_jobs(status);

-- =============================================
-- NOTIFICATIONS
-- =============================================
CREATE TABLE notification_settings (
    id                  UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    telegram_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    telegram_chat_id    VARCHAR(100),
    telegram_bot_token  VARCHAR(255),
    email_enabled       BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled        BOOLEAN NOT NULL DEFAULT FALSE,
    push_subscription   JSONB,
    -- Event preferences
    notify_trade_executed   BOOLEAN NOT NULL DEFAULT TRUE,
    notify_trade_closed     BOOLEAN NOT NULL DEFAULT TRUE,
    notify_sl_hit           BOOLEAN NOT NULL DEFAULT TRUE,
    notify_target_hit       BOOLEAN NOT NULL DEFAULT TRUE,
    notify_daily_report     BOOLEAN NOT NULL DEFAULT TRUE,
    notify_system_alert     BOOLEAN NOT NULL DEFAULT TRUE,
    notify_ai_signal        BOOLEAN NOT NULL DEFAULT TRUE,
    -- Quiet hours
    quiet_start_time    TIME,
    quiet_end_time      TIME,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE notification_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES users(id),
    type            notification_type NOT NULL,
    channel         notification_channel NOT NULL,
    subject         VARCHAR(500),
    body            TEXT NOT NULL,
    metadata        JSONB,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    status          VARCHAR(20) NOT NULL DEFAULT 'SENT',
    error_message   TEXT,
    sent_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_log_user ON notification_log(user_id);
CREATE INDEX idx_notification_log_type ON notification_log(type);
CREATE INDEX idx_notification_log_read ON notification_log(user_id, is_read) WHERE is_read = FALSE;

-- =============================================
-- REPORTS
-- =============================================
CREATE TABLE reports (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id),
    name            VARCHAR(200),
    period          report_period NOT NULL,
    period_start    DATE NOT NULL,
    period_end      DATE NOT NULL,
    -- Summary
    total_trades    INT DEFAULT 0,
    total_pnl       DECIMAL(15,2) DEFAULT 0,
    winning_trades  INT DEFAULT 0,
    losing_trades   INT DEFAULT 0,
    win_rate        DECIMAL(5,2) DEFAULT 0,
    profit_factor   DECIMAL(8,2) DEFAULT 0,
    max_drawdown    DECIMAL(8,2) DEFAULT 0,
    -- Full data
    data            JSONB,
    -- File exports
    pdf_path        VARCHAR(500),
    excel_path      VARCHAR(500),
    csv_path        VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_user ON reports(user_id);
CREATE INDEX idx_reports_period ON reports(period, period_start);

-- =============================================
-- NEWS & SENTIMENT
-- =============================================
CREATE TABLE news_articles (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    title           VARCHAR(500) NOT NULL,
    summary         TEXT,
    content         TEXT,
    source          VARCHAR(100),
    url             VARCHAR(1000),
    category        VARCHAR(50),
    sentiment_score DECIMAL(5,4),
    sentiment_label VARCHAR(20),
    related_symbols TEXT[],
    published_at    TIMESTAMP WITH TIME ZONE,
    fetched_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_news_category ON news_articles(category);
CREATE INDEX idx_news_published ON news_articles(published_at DESC);
CREATE INDEX idx_news_sentiment ON news_articles(sentiment_label);

-- =============================================
-- WATCHLISTS
-- =============================================
CREATE TABLE watchlists (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    instruments     JSONB NOT NULL DEFAULT '[]',
    is_default      BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_watchlists_user ON watchlists(user_id);

-- =============================================
-- SYSTEM CONFIG
-- =============================================
CREATE TABLE system_config (
    key             VARCHAR(100) PRIMARY KEY,
    value           TEXT NOT NULL,
    description     TEXT,
    updated_by      UUID REFERENCES users(id),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Insert default system config
INSERT INTO system_config (key, value, description) VALUES
    ('market.open_time', '09:15', 'Market opening time IST'),
    ('market.close_time', '15:30', 'Market closing time IST'),
    ('trading.max_daily_loss_pct', '5.0', 'Maximum daily loss percentage'),
    ('trading.max_position_size_pct', '10.0', 'Maximum single position size as % of capital'),
    ('trading.default_slippage_pct', '0.1', 'Default slippage percentage for backtesting'),
    ('system.maintenance_mode', 'false', 'System maintenance mode flag');

-- =============================================
-- FUNCTIONS & TRIGGERS
-- =============================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at trigger to all relevant tables
DO $$
DECLARE
    t TEXT;
BEGIN
    FOR t IN
        SELECT unnest(ARRAY[
            'users', 'broker_accounts', 'strategies', 'orders', 'positions',
            'paper_accounts', 'ai_models', 'notification_settings', 'watchlists'
        ])
    LOOP
        EXECUTE format('
            CREATE TRIGGER trigger_%s_updated_at
            BEFORE UPDATE ON %s
            FOR EACH ROW
            EXECUTE FUNCTION update_updated_at_column()', t, t);
    END LOOP;
END;
$$;

-- =============================================
-- SEED DATA
-- =============================================

-- Create default admin user (password: Admin@123)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, status, email_verified)
VALUES (
    uuid_generate_v4(),
    'admin@algotradex.com',
    '$2a$12$LQv3c1yqBo9SkvXS7QkJe.DlQxPYCz6HL4CqXMbWh8psu.4mKGSKi',
    'Admin',
    'User',
    'ADMIN',
    'ACTIVE',
    TRUE
);

-- Insert major Indian market instruments
INSERT INTO instruments (symbol, trading_symbol, name, exchange, instrument_type, lot_size, tick_size) VALUES
    ('NIFTY', 'NIFTY 50', 'Nifty 50 Index', 'NSE', 'INDEX', 50, 0.05),
    ('BANKNIFTY', 'NIFTY BANK', 'Nifty Bank Index', 'NSE', 'INDEX', 25, 0.05),
    ('FINNIFTY', 'NIFTY FIN SERVICE', 'Nifty Financial Services Index', 'NSE', 'INDEX', 40, 0.05),
    ('MIDCPNIFTY', 'NIFTY MID SELECT', 'Nifty Midcap Select Index', 'NSE', 'INDEX', 75, 0.05),
    ('SENSEX', 'SENSEX', 'BSE Sensex Index', 'BSE', 'INDEX', 10, 0.05),
    ('RELIANCE', 'RELIANCE', 'Reliance Industries Ltd', 'NSE', 'EQ', 1, 0.05),
    ('TCS', 'TCS', 'Tata Consultancy Services Ltd', 'NSE', 'EQ', 1, 0.05),
    ('INFY', 'INFY', 'Infosys Ltd', 'NSE', 'EQ', 1, 0.05),
    ('HDFCBANK', 'HDFCBANK', 'HDFC Bank Ltd', 'NSE', 'EQ', 1, 0.05),
    ('ICICIBANK', 'ICICIBANK', 'ICICI Bank Ltd', 'NSE', 'EQ', 1, 0.05),
    ('SBIN', 'SBIN', 'State Bank of India', 'NSE', 'EQ', 1, 0.05),
    ('BHARTIARTL', 'BHARTIARTL', 'Bharti Airtel Ltd', 'NSE', 'EQ', 1, 0.05),
    ('ITC', 'ITC', 'ITC Ltd', 'NSE', 'EQ', 1, 0.05),
    ('KOTAKBANK', 'KOTAKBANK', 'Kotak Mahindra Bank Ltd', 'NSE', 'EQ', 1, 0.05),
    ('LT', 'LT', 'Larsen & Toubro Ltd', 'NSE', 'EQ', 1, 0.05);
