# AlgoTradeX - Production-Ready Algorithmic Trading Platform

This implementation plan outlines the architectural approach and phased execution to build AlgoTradeX, a full-stack, enterprise-level algorithmic trading platform based on the provided requirements.

## Design Decisions
> [!NOTE]
> 1. **Data Source (Migrated)**: We successfully migrated from Yahoo Finance to **DhanHQ v2 API** for ultra-fast, reliable, and real market data for Options, Futures, and Equities.
> 2. **Backtesting Storage**: We fetch historical data on the fly from DhanHQ for backtesting.
> 3. **AI Engine Readiness**: The AI microservice successfully trains XGBoost models using real intraday data from DhanHQ, and seamlessly serves live predictions to the Java backend via REST.

## Proposed Architecture

Following Clean Architecture, DDD, and SOLID principles:

*   **Frontend**: React 19 SPA with TypeScript, Vite, Material UI, Redux/React Query, and Recharts/AG Grid for complex data rendering.
*   **Backend Core**: Java 21 Spring Boot 3 monolith (microservice-ready). Handles users, strategy logic, broker interfaces, portfolio tracking, and STOMP WebSockets.
*   **AI Microservice**: Python FastAPI. Dedicated to fetching historical data via DhanHQ, training XGBoost models, and serving real-time predictions.
*   **Database**: PostgreSQL (relational data, users, orders, strategies) + Redis (caching).

## Execution Phases

### Phase 1: Core Backend & Security ✅ (Completed)
*   PostgreSQL schema, JWT Authentication, Registration, Login, Role-based access.

### Phase 2: Frontend Foundation & Dashboard ✅ (Completed)
*   React 19 + TypeScript, Authentication UI, Dashboard layout.

### Phase 3: Broker Integration Adapter ✅ (Completed)
*   `BrokerAdapter` interface (Dhan API integration for profile, positions, and orders).

### Phase 4: Market Data & WebSockets ✅ (Completed)
*   Spring Boot STOMP WebSocket broker at `/ws` for live tick streaming.
*   TradingView Lightweight Charts integrated.

### Phase 5: Algo Execution Engine ✅ (Completed)
*   `Strategy.java` model, Technical Indicators (SMA, RSI).
*   `OrderExecutionEngine` connected to broker adapters for automated orders.
*   Premium Strategies Management Dashboard in React.

### Phase 6: Backtesting & Paper Trading ✅ (Completed)
*   Implemented `BacktestEngine.java` to simulate strategy evaluation on historical data.
*   Frontend "Run Backtest" wizard with Recharts equity curve visualization.

### Phase 7: AI Module (FastAPI & XGBoost) ✅ (Completed)
*   Initialized Python FastAPI microservice.
*   Implemented Scikit-learn/XGBoost pipelines for training on historical intraday option data.
*   Exposed `/models/train` and `/models/predict` endpoints.

### Phase 8: Model Persistence & Feature Engineering ✅ (Completed)
*   Implemented `joblib` model saving/loading.
*   Calculated EMAs, RSI, and MACD in Pandas for robust AI feature pipelines.

### Phase 9: AI Integration in Execution Engine ✅ (Completed)
*   Integrated Java Backend `AIPredictionService` to call the Python AI microservice.
*   Created `AIBasedOptionStrategy` that evaluates AI probabilities before executing live trades.

### Phase 10: DhanHQ Migration & Stabilization ✅ (Completed)
*   Completely stripped out Yahoo Finance.
*   Integrated `InstrumentRepository` to dynamically resolve actual Dhan exchange mapping tokens.
*   Refactored `HistoricalDataService` and `RealMarketDataStreamer` to pull ultra-fast JSON charts directly from Dhan.
*   Fixed chart limits to load full intraday sessions and sync perfectly with WebSockets.

### Phase 11: CI/CD & Deployment (Next Phase)
*   Finalize Dockerfiles and `docker-compose.yml`.
*   Setup GitHub Actions for automated testing and building.

## Verification Plan
1. **Automated Tests**: JUnit/Mockito tests for core services (especially Order Management and Strategy logic).
2. **Manual Verification**: We have successfully verified end-to-end functionality including live WebSocket ticking, Backtesting execution, AI training, and Option Strategy prediction utilizing live Dhan API data.
