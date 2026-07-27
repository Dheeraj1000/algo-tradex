# AlgoTradeX

A production-ready algorithmic trading platform designed for Indian Equity and Derivative markets (NSE/BSE).

## Architecture
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS, Zustand, Framer Motion
- **Backend**: Java 21, Spring Boot 3, Spring Security (JWT)
- **Database**: PostgreSQL 18.4, Flyway Migrations
- **Cache**: In-memory (Redis disabled for local dev)

## Current Status
- **Phase 1 (Backend Core & Security)**: ✅ Completed
- **Phase 2 (Frontend Foundation & Dashboard)**: ✅ Completed
- **Phase 3 (Broker Integration & Trading UI)**: ✅ Completed
- **Phase 4 (Market Data & WebSockets)**: 🚧 Pending
- **Phase 5 (Strategy Builder & Backtesting)**: 🚧 Pending
- **Phase 6 (Live & Paper Trading)**: 🚧 Pending
- **Phase 7 (AI Module)**: 🚧 Pending

## Local Setup Instructions

Ensure you have Java 21, Node.js, and PostgreSQL running locally on port `5432` with a database named `algotradex`.

### 1. Start the Backend
Navigate to the `backend` directory and run using Maven daemon:
```bash
cd backend
mvnd clean spring-boot:run "-Dspring-boot.run.profiles=local"
```
The backend API runs on `http://localhost:8080/api`
Swagger UI is available at `http://localhost:8080/swagger-ui.html`

### 2. Start the Frontend
Navigate to the `frontend` directory and start the Vite dev server:
```bash
cd frontend
npm install
npm run dev
```
The application runs on `http://localhost:5173/`

## Features Completed
- Complete Authentication flow (JWT, Registration, Login)
- Dynamic Dashboard with Dark Theme and Glassmorphism
- Broker Integration UI (Mock broker support)
- Manual Trading Order Ticket (Indian Market structure: MIS/CNC)
- Portfolio Monitoring Grid (MTM P&L, Available Margin)
