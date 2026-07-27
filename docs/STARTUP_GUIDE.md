# Starting AlgoTradeX Services

This document explains how to start the AlgoTradeX ecosystem, which consists of three main components: the AI Service (Python/FastAPI), the Backend (Java/Spring Boot), and the Frontend (React/Vite).

## Quick Start (Recommended)

To launch all services simultaneously on Windows, we have provided convenient batch scripts in the root directory:

1. Open your File Explorer.
2. Navigate to the root folder of the project (`algotradex`).
3. Double-click **`run_all.bat`**.

This script will automatically open separate terminal windows for each of the services (AI FastAPI, AI Market Monitor, Spring Boot Backend, and React Frontend) and start them up.

## Manual Startup

If you prefer to start the services manually or need to view the logs in a specific terminal, you can run them individually:

### 1. AI Service & Market Monitor
The AI component powers the market predictions and the live market monitoring script.
```bash
# Open a terminal and navigate to the ai-service folder
cd ai-service

# Start the FastAPI Server (Port 8001)
python -m uvicorn main:app --port 8001

# In a separate terminal, start the Live Market Monitor for Indian Market
python market_monitor.py

# Or start the Crypto Market Monitor for Binance Perpetual Futures
python crypto_monitor.py
```

### 2. Java Backend
The Spring Boot backend handles trading logic, database operations, and websocket broadcasting.
```bash
# Open a terminal and navigate to the backend folder
cd backend

# Start the Spring Boot application (Port 8080)
mvn spring-boot:run
```

### 3. React Frontend
The React/Vite frontend provides the beautiful UI dashboard.
```bash
# Open a terminal and navigate to the frontend folder
cd frontend

# Start the development server
npm run dev
```

## Troubleshooting

- **Port Conflicts:** Ensure that ports `8080` (Backend), `8001` (AI Service), and `5173` (Vite Default) are free before starting.
- **Missing Dependencies:**
  - AI Service: Ensure your Python virtual environment is active and you have run `pip install -r requirements.txt`.
  - Frontend: If you encounter missing module errors, run `npm install` inside the `frontend` folder.
  - Backend: Maven will automatically download dependencies on the first run.
- **AI Alert Widget Not Showing:** Ensure both the Backend and the AI Market Monitor are running. The monitor requires the backend to broadcast signals to the frontend via WebSockets.
