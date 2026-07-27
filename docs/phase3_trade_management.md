# AlgoTradeX - Phase 3: AI Trade Management Engine

This document outlines the architecture and implementation of **Phase 3** of the AlgoTradeX platform, focusing on the highly intelligent, continuous AI Trade Management Engine. 

While Phase 2 focused on generating Entry Signals, Phase 3 focuses on what happens *after* a trade is entered, specifically targeting dynamic exits, recovery prediction, and trailing stop losses.

## Core Objective
The engine must not blindly exit when a hard stop-loss is touched if normal market volatility is the cause. Instead, it continuously analyzes live market conditions (every tick) and determines whether the active position should:
* `HOLD` (Recovery probability is high)
* `EXIT` (Recovery probability is low)
* `MOVE_STOP_LOSS` (Trail to lock in profits)
* `PARTIAL_EXIT` (Take partial profits)
* `REENTER` (Setup valid again after stop loss hit)

## Architecture Flow

1. **Market Data Stream (Dhan API)** -> Continuous flow of live Options and Spot data.
2. **Spring Boot (TradeManagementMonitor)** -> A high-frequency scheduled task that loops through all `OPEN` positions in the database.
3. **AI Decision Engine (FastAPI `/models/manage_trade`)** -> Evaluates the position state and the live market data to output a decision and a text explanation.
4. **Risk Engine (Spring Boot `RiskEngineService`)** -> Intercepts actions to ensure Max Daily Loss, Max Exposure, and Margin rules are respected.
5. **Execution Engine (BrokerService)** -> Fires the API commands to the broker to exit or modify the trade.

## Database Entities (PostgreSQL)

*   **`TradeSignal`**: Expanded to include management fields (`recovery_probability`, `exit_reason`, `ai_explanation`, `current_stop_loss`).
*   **`PositionState`**: A new entity for tracking active scale-in/scale-outs, tracking `current_quantity`, `partial_exits_count`, and `reentry_count`.
*   **`TradeManagementLog`**: A high-volume table that records every tick's decision for a specific trade, providing an auditable trail of *why* the AI chose to hold or exit at a specific timestamp.

## The Recovery Engine Logic

If the live price breaches the `currentStopLoss`, the `TradeManagementMonitor` does NOT immediately fire a market exit order.
Instead, it sets `stop_loss_breached = True` in the payload sent to the AI Service.

The AI Service (Python):
1. Calculates live features (OI shifts, VWAP, ATR, SuperTrend).
2. Predicts the `recovery_probability`.
3. If `recovery_probability > Configured_Threshold`, the AI returns `HOLD` with an explanation.
4. If it returns `EXIT`, the Spring Boot backend fires the broker exit.

## Explainable AI

For every single action, the AI engine returns a human-readable explanation that is stored in the database and shown on the React Dashboard. 
*Example:* `Holding position despite SL breach. Recovery probability is 72% based on current OI support at 24400.`

## Frontend Dashboard

A new dedicated route (`/trade-management`) exists in the React frontend. It provides:
1. Live counters for Active Trades and Premature Exits Saved.
2. A live streaming feed of every managed trade, showing its current Recovery Probability and the latest AI Decision text.
3. Configuration dials in the `Settings` page to adjust the Recovery Probability Threshold and Exit Confirmation Delays.
