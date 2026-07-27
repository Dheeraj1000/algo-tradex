# Phase 1: Core Backend & Security
- `[x]` Finalize PostgreSQL schema using Flyway migrations
- `[x]` Complete JWT Authentication, Registration, Login, and Role-based access
- `[x]` Set up Global Exception Handling and Standard API Responses

# Phase 2: Frontend Foundation & Dashboard
- `[x]` Initialize React 19 + TypeScript project
- `[x]` Setup Material UI theme, React Router, and React Query
- `[x]` Build Authentication UI (Login, Signup, 2FA)

# Phase 3: Broker Integration Adapter
- `[x]` Create `BrokerAdapter` Java interface
- `[x]` Implement Dhan adapter workflows (Place, Modify, Cancel, Portfolio)

# Phase 4: Market Data & WebSockets
- `[x]` Exposed historical candles API endpoint
- `[x]` Implemented Spring Boot STOMP WebSocket broker at `/ws` for live tick streaming
- `[x]` Integrated TradingView Lightweight Charts on the UI

# Phase 5: Algo Execution Engine
- `[x]` Extend `Strategy.java` model to support instruments and rule parameters
- `[x]` Implement technical indicator signal generators
- `[x]` Connect `OrderExecutionEngine` to broker adapters for automated trading

# Phase 6: Backtesting & Paper Trading
- `[x]` Implement `BacktestEngine` and `BacktestResult` models
- `[x]` Enhance `OrderExecutionEngine` for proper paper trading tracking
- `[x]` Add "Run Backtest" wizard to the Strategies Dashboard frontend

# Phase 7: AI Module
- `[x]` Initialize Python FastAPI project
- `[x]` Implement data pipelines using Pandas/NumPy
- `[x]` Create endpoints for training and prediction using Scikit-learn/XGBoost

# Phase 8: Model Persistence & Feature Engineering
- `[x]` Setup Joblib persistence for XGBoost and Scalers
- `[x]` Implement dynamic Feature Engineering (EMA, MACD, RSI, Volatility)

# Phase 9: AI Integration in Execution Engine
- `[x]` Build `AIPredictionService` in Java Spring Boot
- `[x]` Create `AIBasedOptionStrategy` that hits FastAPI for trading signals

# Phase 10: DhanHQ Migration
- `[x]` Strip out Yahoo Finance and integrate DhanHQ v2 historical API
- `[x]` Integrate `InstrumentRepository` for dynamic Dhan security mapping
- `[x]` Plumb Live STOMP streaming to hit Dhan API and bucket 1-minute real-time charts
