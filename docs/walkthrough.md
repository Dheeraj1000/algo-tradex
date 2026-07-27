# AlgoTradeX Walkthrough

## Local Development Setup

### Prerequisites
- **Java 21** (Oracle JDK 21.0.10)
- **Maven Daemon** (`mvnd`) for fast builds
- **Node.js** with npm
- **PostgreSQL 18.4** (running locally, database: `algotradex`)

### Starting Servers (No Docker)

**Backend (Spring Boot 3 on port 8080):**
```bash
cd backend
mvnd spring-boot:run "-Dspring-boot.run.profiles=local"
```
- Uses `application-local.yml` profile which disables Redis (uses in-memory cache instead)
- Connects to local PostgreSQL at `localhost:5432/algotradex`
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator/health

**Frontend (Vite dev server on port 5173):**
```bash
cd frontend
npm run dev
```
- Runs at http://localhost:5173/
- API calls proxy to `http://localhost:8080/api`
- Has mock login fallback if backend is unreachable

---

## Phase 1: Core Backend & Security ✅

### What was Accomplished

#### 1. Robust PostgreSQL Schema
- The Flyway migration script (`V1__init_schema.sql`) contains the full enterprise schema. 
- It covers `users`, `broker_accounts`, `strategies`, `orders`, `market_data_candles`, `ai_models`, and `backtests`.
- It includes optimized indexing and UUID primary keys for security.

#### 2. JWT & Auth Flow
- Implemented `JwtTokenProvider` to generate and validate stateless Access and Refresh tokens.
- Created `AuthService` handling registration and login workflows.
- Implemented `/api/auth/register` and `/api/auth/login` in `AuthController`.

#### 3. 2FA (TOTP) Support
- Integrated the `dev.samstevens.totp` library for time-based one-time passwords (like Google Authenticator).
- Created a stubbed `TwoFactorAuthService` hooked up to the login flow. If a user has 2FA enabled, the login endpoint will ask for the TOTP code.

#### 4. Enterprise Standards
- Implemented `GlobalExceptionHandler` to elegantly catch and wrap exceptions into standard `ApiResponse` format.
- Integrated `springdoc-openapi-starter-webmvc-ui` and created `OpenApiConfig` to enable Swagger UI at `/swagger-ui.html` with Bearer token authentication support.
- Added basic JUnit tests for the `AuthController` utilizing MockMvc to verify contract integrity.

---

## Phase 2: Frontend Foundation & Dashboard ✅

### What was Accomplished

#### 1. Core Infrastructure
- React 19 + TypeScript + Vite setup with `react-router-dom`, `@tanstack/react-query`, `axios`, `zustand`, `lucide-react`, `framer-motion`.
- Axios instance with JWT interceptor and automatic token refresh on 401.

#### 2. Design System
- Premium dark theme with CSS variables (deep navy/black palette).
- Glassmorphism utilities, gradient text, custom scrollbar, Inter font.
- CSS Modules for component-scoped styling.

#### 3. Reusable UI Components
- `Button` — primary/secondary/ghost variants with Framer Motion tap animation and loading spinner.
- `Input` — styled inputs with left icon support and floating labels.
- `Card` — glass-panel cards with hover effects.

#### 4. Authentication Flow
- Zustand auth store with `persist` middleware (survives page refresh).
- **Login page** with email/password, mock fallback for demo, and links to register.
- **Register page** with full sign-up form.
- `ProtectedRoute` wrapper that redirects unauthenticated users to `/login`.

#### 5. Dashboard Layout
- Animated sidebar with `NavLink` active indicator (spring animation via `layoutId`).
- Top header with search bar, notification bell, and user avatar.
- Responsive grid dashboard with stat cards, portfolio chart placeholder, and active orders list.

---

## Phase 3: Broker Integration Adapter ✅

### What was Accomplished

#### 1. Backend Core Trading Domain
- Created fully mapped JPA Entities for `BrokerAccount`, `Instrument`, `Order`, `Trade`, and `Position`.
- Generated custom PostgreSQL Enum mappings (e.g. `BrokerType`, `OrderStatus`, `ProductType`, `InstrumentType`) utilizing `@JdbcTypeCode`.
- Designed the `BrokerAdapter` interface (SPI) for loose coupling between AlgoTradeX and external broker APIs.
- Implemented a `MockBrokerAdapter` with basic stub implementations for local end-to-end testing without real keys.
- Exposed comprehensive REST endpoints via `BrokerController`, `OrderController`, and `PortfolioController`.

#### 2. Frontend Trading Experience
- **Settings Module**: Added a UI to list connected broker accounts and a glassmorphism form to link a new broker (supporting Mock, Zerodha, Dhan, etc).
- **Trading Module**: Built a comprehensive Manual Order Ticket specifically tailored for the Indian Market. Supports Segment (NSE/BSE), Product Types (MIS/CNC/NRML), and Order Types (Market/Limit).
- **Portfolio Module**: Created an analytical view of current holdings/positions featuring an AG Grid-style display with metrics for LTP, Unrealized P&L, and Available Margin.

*Note: All development strictly adheres to Indian market standards (NSE/BSE focus) avoiding crypto currency instruments.*

---

## Phase 11: Crypto Algorithmic Execution Engine ✅

### What was Accomplished

#### 1. Real-Time Python AI Monitor
- Developed `crypto_monitor.py` as an independent microservice that continuously polls Binance endpoints (using `ccxt`) for live 1m BTCUSDT and ETHUSDT data.
- Built a localized `XGBoost` model loop that runs predictions against historical signals.
- Configured dynamic Stop Loss and Take Profit levels leveraging Average True Range (ATR) directly routed to the Java backend over HTTP.

#### 2. Robust Java Execution & Tracking Layer
- Created `CryptoStrategyEvaluator` to ingest Python signals and generate `TradeSignal` entities only if the AI confidence meets strict thresholds (e.g. `> 75%`).
- Established robust deduplication constraints to prevent catastrophic duplicate positions stacking.
- Built `TradeManagementMonitor.java`, a `@Scheduled` background worker that runs every 5 seconds to manage live open trades, update Stop Losses, and handle partial or complete automated exits.

#### 3. Complete Compounding Architecture
- Implemented `CryptoPortfolioController` which accurately calculates cumulative portfolio value starting from a pristine ₹1,00,000 base.
- Position sizing uses 100% of available compounded equity for each trade to simulate true exponential algorithmic growth.
- Currency formatting dynamically translates Binance USD metrics into precise `en-IN` Rupee valuations (using `96.14` USD/INR exchange rate).

#### 4. Frontend Command Center
- Constructed a gorgeous two-column glassmorphism UI in `CryptoTrading.tsx` utilizing modern Tailwind layout patterns.
- **Left Column (Live Stream)**: Visually feeds AI analytical breakdowns, Target limits, Stop Loss limits, and Confidence metrics.
- **Right Column (Order History)**: Archives completely processed `CLOSED` trades providing full post-mortem clarity (Realized P&L, Investment allocated, Exit reasoning).
- Built high-performance React grids to display instantaneous Active Trades tracking real-time entry margins vs dynamic live prices.
