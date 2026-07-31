from fastapi import FastAPI, BackgroundTasks
import yfinance as yf
from pydantic import BaseModel
from typing import Optional
import os
import joblib
from xgboost import XGBClassifier
from sklearn.model_selection import train_test_split, TimeSeriesSplit, GridSearchCV
from sklearn.metrics import accuracy_score
from ml_utils import compute_features, get_feature_columns

app = FastAPI(title="AlgoTradeX AI Service", version="1.0")

MODELS_DIR = "models"
os.makedirs(MODELS_DIR, exist_ok=True)

class IngestRequest(BaseModel):
    symbol: str
    period: str = "1d"
    interval: str = "1m"

class TrainRequest(BaseModel):
    symbol: str
    period: str = "60d"
    interval: str = "5m"
    client_id: Optional[str] = None
    access_token: Optional[str] = None

class TrainCryptoRequest(BaseModel):
    symbol: str # BTCUSDT, ETHUSDT
    period: str = "5d"
    interval: str = "1m"

class PredictRequest(BaseModel):
    symbol: str
    client_id: Optional[str] = None
    access_token: Optional[str] = None

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "ai-service"}

@app.get("/")
def root():
    return {"message": "AlgoTradeX AI Service is running"}

def get_yahoo_symbol(symbol):
    if symbol.upper() == "NIFTY":
        return "^NSEI"
    elif symbol.upper() == "SENSEX":
        return "^BSESN"
    elif symbol.startswith("^"):
        return symbol
    elif symbol.endswith(".NS"):
        return symbol
    else:
        return f"{symbol}.NS"

@app.post("/data/ingest")
def ingest_data(req: IngestRequest):
    yahoo_symbol = get_yahoo_symbol(req.symbol)
    ticker = yf.Ticker(yahoo_symbol)
    df = ticker.history(period=req.period, interval=req.interval)
    
    if df.empty:
        return {"status": "error", "message": f"No data found for {yahoo_symbol}"}
        
    return {
        "status": "success",
        "symbol": yahoo_symbol,
        "rows_fetched": len(df),
        "latest_close": df.iloc[-1]['Close'],
        "message": "Data ingested successfully and ready for AI analysis."
    }

def fetch_dhan_data(client_id, access_token, symbol, period, interval):
    try:
        from dhanhq import dhanhq, DhanContext
        import pandas as pd
        from datetime import datetime, timedelta
        from data.dhan_master_downloader import get_atm_option
        
        ctx = DhanContext(client_id, access_token)
        dhan = dhanhq(ctx)
        
        security_id = "13" # Default NIFTY
        exchange_segment = "IDX_I"
        
        if symbol.upper() == "SENSEX":
            security_id = "51"
        elif symbol.upper() == "NIFTY BANK" or symbol.upper() == "BANKNIFTY":
            security_id = "25"
            
        to_date = datetime.now().strftime("%Y-%m-%d")
        
        days = 5
        if period.endswith('d'):
            try:
                days = int(period[:-1])
            except:
                pass
                
        days = min(days, 5) 
        from_date = (datetime.now() - timedelta(days=days)).strftime("%Y-%m-%d")
        
        int_map = {"1m": 1, "5m": 5, "15m": 15, "60m": 60}
        dhan_int = int_map.get(interval, 5)
        
        res = dhan.intraday_minute_data(
            security_id=security_id,
            exchange_segment=exchange_segment,
            instrument_type="INDEX",
            from_date=from_date,
            to_date=to_date,
            interval=dhan_int
        )
        
        if res.get("status") == "success" and "data" in res:
            data = res["data"]
            df = pd.DataFrame({
                'Timestamp': data.get('timestamp', []),
                'Open': data['open'],
                'High': data['high'],
                'Low': data['low'],
                'Close': data['close'],
                'Volume': data['volume']
            })
            if not df.empty:
                print(f"[{symbol}] Fetched {len(df)} rows from Dhan.")
                
                # Fetch Option Chain (OI) Sentiment
                df['CE_OI'] = 1.0 # default to avoid div by zero
                df['PE_OI'] = 1.0
                try:
                    spot_price = df['Close'].iloc[-1]
                    ce_info = get_atm_option(symbol, spot_price, "CE")
                    pe_info = get_atm_option(symbol, spot_price, "PE")
                    
                    if ce_info and pe_info:
                        ce_res = dhan.intraday_minute_data(
                            security_id=ce_info['security_id'],
                            exchange_segment=ce_info['exchange_segment'],
                            instrument_type="OPTIDX",
                            from_date=from_date,
                            to_date=to_date,
                            interval=dhan_int,
                            oi=True
                        )
                        pe_res = dhan.intraday_minute_data(
                            security_id=pe_info['security_id'],
                            exchange_segment=pe_info['exchange_segment'],
                            instrument_type="OPTIDX",
                            from_date=from_date,
                            to_date=to_date,
                            interval=dhan_int,
                            oi=True
                        )
                        
                        if ce_res.get("status") == "success" and "data" in ce_res:
                            ce_df = pd.DataFrame({'Timestamp': ce_res["data"].get('timestamp', []), 'CE_OI': ce_res["data"].get('open_interest', [])})
                            df = pd.merge(df, ce_df, on='Timestamp', how='left')
                            df['CE_OI'] = df['CE_OI_y'].fillna(df['CE_OI_x']).fillna(1.0)
                            df = df.drop(columns=['CE_OI_x', 'CE_OI_y'], errors='ignore')
                            
                        if pe_res.get("status") == "success" and "data" in pe_res:
                            pe_df = pd.DataFrame({'Timestamp': pe_res["data"].get('timestamp', []), 'PE_OI': pe_res["data"].get('open_interest', [])})
                            df = pd.merge(df, pe_df, on='Timestamp', how='left')
                            df['PE_OI'] = df['PE_OI_y'].fillna(df['PE_OI_x']).fillna(1.0)
                            df = df.drop(columns=['PE_OI_x', 'PE_OI_y'], errors='ignore')
                except Exception as oi_err:
                    print(f"[{symbol}] Error fetching OI: {oi_err}")
                return df
        return pd.DataFrame()
    except Exception as e:
        print(f"Error fetching Dhan data: {e}")
        return pd.DataFrame()

def fetch_binance_data(symbol, period, interval):
    import requests
    import pandas as pd
    
    # Binance limit is 1000 candles per request.
    # 1000 candles at 1m interval = ~16 hours of data, plenty for feature computation.
    limit = 1000
    url = f"https://api.binance.com/api/v3/klines?symbol={symbol}&interval={interval}&limit={limit}"
    
    try:
        res = requests.get(url, timeout=5)
        if res.status_code == 200:
            data = res.json()
            df = pd.DataFrame(data, columns=[
                'OpenTime', 'Open', 'High', 'Low', 'Close', 'Volume',
                'CloseTime', 'QuoteAssetVolume', 'NumberOfTrades',
                'TakerBuyBaseVolume', 'TakerBuyQuoteVolume', 'Ignore'
            ])
            df['Timestamp'] = pd.to_datetime(df['OpenTime'], unit='ms')
            df['Open'] = df['Open'].astype(float)
            df['High'] = df['High'].astype(float)
            df['Low'] = df['Low'].astype(float)
            df['Close'] = df['Close'].astype(float)
            df['Volume'] = df['Volume'].astype(float)
            
            # Mock OI data since we trade crypto perpetual futures, not options
            df['CE_OI'] = 1.0
            df['PE_OI'] = 1.0
            
            print(f"[{symbol}] Fetched {len(df)} rows from Binance.")
            return df
        else:
            print(f"[{symbol}] Binance API Error: {res.text}")
            return pd.DataFrame()
    except Exception as e:
        print(f"[{symbol}] Error fetching Binance data: {e}")
        return pd.DataFrame()

def background_train(req: TrainRequest):
    try:
        req_symbol = req.symbol
        print(f"[{req_symbol}] Training on Dhan Spot Data...")
        
        if not req.client_id or not req.access_token:
            print(f"[{req_symbol}] Error: Missing Dhan credentials for training.")
            return
            
        df = fetch_dhan_data(req.client_id, req.access_token, req.symbol, req.period, req.interval)
        
        if df.empty:
            print(f"[{req_symbol}] No spot data found from Dhan.")
            return
            
        df_features = compute_features(df)
        features = get_feature_columns()
        
        # 1. Train Trend-Following Model
        df_trend = df_features.dropna(subset=['Target_Trend'])
        if len(df_trend) >= 100:
            X_trend = df_trend[features]
            y_trend = df_trend['Target_Trend']
            tscv = TimeSeriesSplit(n_splits=3)
            param_grid = {'max_depth': [3, 5], 'learning_rate': [0.05, 0.1], 'n_estimators': [50, 100]}
            pos_weight_trend = float((y_trend == 0).sum()) / max(1, float((y_trend == 1).sum()))
            base_model_trend = XGBClassifier(random_state=42, eval_metric='logloss', scale_pos_weight=pos_weight_trend)
            grid_trend = GridSearchCV(estimator=base_model_trend, param_grid=param_grid, cv=tscv, scoring='accuracy', n_jobs=-1)
            grid_trend.fit(X_trend, y_trend)
            joblib.dump(grid_trend.best_estimator_, os.path.join(MODELS_DIR, f"{req_symbol.upper()}_trend_xgb.joblib"))
        
        # 2. Train Mean-Reversion Model
        df_meanrev = df_features.dropna(subset=['Target_MeanRev'])
        if len(df_meanrev) >= 100:
            X_meanrev = df_meanrev[features]
            y_meanrev = df_meanrev['Target_MeanRev']
            pos_weight_meanrev = float((y_meanrev == 0).sum()) / max(1, float((y_meanrev == 1).sum()))
            base_model_meanrev = XGBClassifier(random_state=42, eval_metric='logloss', scale_pos_weight=pos_weight_meanrev)
            grid_meanrev = GridSearchCV(estimator=base_model_meanrev, param_grid=param_grid, cv=tscv, scoring='accuracy', n_jobs=-1)
            grid_meanrev.fit(X_meanrev, y_meanrev)
            joblib.dump(grid_meanrev.best_estimator_, os.path.join(MODELS_DIR, f"{req_symbol.upper()}_meanrev_xgb.joblib"))
        
        print(f"[{req_symbol}] Advanced Training Complete | Both Trend & MeanRev models saved!")
    except Exception as e:
        print(f"[{req_symbol}] Error in training: {e}")

@app.post("/models/train")
def train_model(req: TrainRequest, background_tasks: BackgroundTasks):
    background_tasks.add_task(background_train, req)
    
    return {
        "status": "success",
        "symbol": req.symbol,
        "message": f"Advanced training started in background for {req.symbol} using Dhan."
    }

def background_train_crypto(req: TrainCryptoRequest):
    try:
        req_symbol = req.symbol.upper()
        print(f"[{req_symbol}] Training on Binance Spot Data...")
            
        df = fetch_binance_data(req.symbol, req.period, req.interval)
        
        if df.empty:
            print(f"[{req_symbol}] No spot data found from Binance.")
            return
            
        df_features = compute_features(df)
        features = get_feature_columns()
        
        # 1. Train Trend-Following Model
        df_trend = df_features.dropna(subset=['Target_Trend'])
        if len(df_trend) >= 100:
            X_trend = df_trend[features]
            y_trend = df_trend['Target_Trend']
            tscv = TimeSeriesSplit(n_splits=3)
            param_grid = {'max_depth': [3, 5], 'learning_rate': [0.05, 0.1], 'n_estimators': [50, 100]}
            pos_weight_trend = float((y_trend == 0).sum()) / max(1, float((y_trend == 1).sum()))
            base_model_trend = XGBClassifier(random_state=42, eval_metric='logloss', scale_pos_weight=pos_weight_trend)
            grid_trend = GridSearchCV(estimator=base_model_trend, param_grid=param_grid, cv=tscv, scoring='accuracy', n_jobs=-1)
            grid_trend.fit(X_trend, y_trend)
            joblib.dump(grid_trend.best_estimator_, os.path.join(MODELS_DIR, f"{req_symbol}_trend_xgb.joblib"))
        
        # 2. Train Mean-Reversion Model
        df_meanrev = df_features.dropna(subset=['Target_MeanRev'])
        if len(df_meanrev) >= 100:
            X_meanrev = df_meanrev[features]
            y_meanrev = df_meanrev['Target_MeanRev']
            pos_weight_meanrev = float((y_meanrev == 0).sum()) / max(1, float((y_meanrev == 1).sum()))
            base_model_meanrev = XGBClassifier(random_state=42, eval_metric='logloss', scale_pos_weight=pos_weight_meanrev)
            grid_meanrev = GridSearchCV(estimator=base_model_meanrev, param_grid=param_grid, cv=tscv, scoring='accuracy', n_jobs=-1)
            grid_meanrev.fit(X_meanrev, y_meanrev)
            joblib.dump(grid_meanrev.best_estimator_, os.path.join(MODELS_DIR, f"{req_symbol}_meanrev_xgb.joblib"))
        
        print(f"[{req_symbol}] Crypto Training Complete | Models saved!")
    except Exception as e:
        print(f"[{req_symbol}] Error in crypto training: {e}")

@app.post("/models/train_crypto")
def train_crypto_model(req: TrainCryptoRequest, background_tasks: BackgroundTasks):
    background_tasks.add_task(background_train_crypto, req)
    
    return {
        "status": "success",
        "symbol": req.symbol,
        "message": f"Crypto training started in background for {req.symbol} using Binance."
    }

@app.post("/models/predict")
def predict(req: PredictRequest):
    req_symbol = req.symbol.upper()
    trend_model_path = os.path.join(MODELS_DIR, f"{req_symbol}_trend_xgb.joblib")
    meanrev_model_path = os.path.join(MODELS_DIR, f"{req_symbol}_meanrev_xgb.joblib")
    
    if not os.path.exists(trend_model_path) or not os.path.exists(meanrev_model_path):
        # Fallback to old path if both don't exist
        old_path = os.path.join(MODELS_DIR, f"{req_symbol}_xgb.joblib")
        if not os.path.exists(old_path):
            return {"status": "error", "message": "Models not found. Train first.", "confidence_score": 50.0}
        trend_model = joblib.load(old_path)
        meanrev_model = trend_model
    else:
        trend_model = joblib.load(trend_model_path)
        meanrev_model = joblib.load(meanrev_model_path)
        
    if not req.client_id or not req.access_token:
        return {"status": "error", "message": "Missing Dhan credentials for prediction.", "confidence_score": 50.0}
        
    # Predict over last 5 days just to generate features
    df = fetch_dhan_data(req.client_id, req.access_token, req.symbol, "5d", "5m")
    
    if df.empty:
        return {"status": "error", "message": "Dhan API returned empty data. Skipping.", "confidence_score": 0.0}
        
    df_features = compute_features(df)
    if df_features.empty:
        return {"status": "error", "message": "Not enough recent data for features", "confidence_score": 50.0}
        
    features = get_feature_columns()
    latest_features = df_features[features].iloc[-1:]
    
    # Evaluate Trend Model
    prob_trend = trend_model.predict_proba(latest_features)[0][1]
    conf_trend = prob_trend if prob_trend > 0.5 else (1 - prob_trend)
    
    # Evaluate Mean Reversion Model
    prob_meanrev = meanrev_model.predict_proba(latest_features)[0][1]
    conf_meanrev = prob_meanrev if prob_meanrev > 0.5 else (1 - prob_meanrev)
    
    # Pick the strategy with the highest confidence
    if conf_trend >= conf_meanrev:
        prob = prob_trend
        strategy_used = "TREND_FOLLOWING"
    else:
        prob = prob_meanrev
        strategy_used = "MEAN_REVERSION"
        
    # AI Output Filter (Prevent trading against the trend)
    current_spot = float(df['Close'].iloc[-1])
    current_sma_10 = float(df_features['SMA_10'].iloc[-1]) if 'SMA_10' in df_features else current_spot
    current_sma_20 = float(df_features['SMA_20'].iloc[-1]) if 'SMA_20' in df_features else current_spot
    current_macd_hist = float(df_features['MACD_Hist'].iloc[-1]) if 'MACD_Hist' in df_features else 0.0
    
    if prob > 0.55: # AI wants to BUY_CALL
        if current_spot < current_sma_10 or (current_spot < current_sma_20 and current_macd_hist < 0):
            prob = 0.5 # Downgrade to HOLD because market is crashing (below 10-SMA) or macro trend is down
        elif current_spot > current_sma_10 and current_spot > current_sma_20 and current_macd_hist > 0:
            prob = 0.85 # BOOST to 85% confidence if perfectly aligned with UPTREND
    elif prob < 0.45: # AI wants to BUY_PUT
        if current_spot > current_sma_10 or (current_spot > current_sma_20 and current_macd_hist > 0):
            prob = 0.5 # Downgrade to HOLD because market is pumping (above 10-SMA) or macro trend is up
        elif current_spot < current_sma_10 and current_spot < current_sma_20 and current_macd_hist < 0:
            prob = 0.15 # BOOST to 85% confidence (1 - 0.15 = 0.85) if perfectly aligned with DOWNTREND
    else: # AI is UNSURE
        if current_spot > current_sma_10 and current_spot > current_sma_20 and current_macd_hist > 0:
            prob = 0.85 # Trend is UP, override AI uncertainty
        elif current_spot < current_sma_10 and current_spot < current_sma_20 and current_macd_hist < 0:
            prob = 0.15 # Trend is DOWN, override AI uncertainty
    
    import news_sentiment
    sentiment_score = news_sentiment.get_market_sentiment()
    
    if prob > 0.55:
        # If news is bearish, only block the CALL if AI confidence is not extremely high (< 80%)
        if sentiment_score < -0.1 and prob < 0.80:
            signal = "HOLD"
            option_type = None
            prob = 0.5
            strategy_used = "BEARISH_NEWS_BLOCKED_CALL"
        else:
            signal = "BUY_CALL"
            option_type = "CE"
    elif prob < 0.45:
        signal = "BUY_PUT"
        option_type = "PE"
    else:
        signal = "HOLD"
        option_type = None

    target_symbol = None
    target_ltp = 0.0
    recommended_entry = 0.0
    if signal != "HOLD":
        from data.dhan_master_downloader import get_atm_option
        spot = df['Close'].iloc[-1]
        opt_info = get_atm_option(req.symbol, spot, option_type)
        if opt_info:
            target_symbol = opt_info['trading_symbol']
            try:
                from dhanhq import dhanhq, DhanContext
                from datetime import datetime, timedelta
                ctx = DhanContext(req.client_id, req.access_token)
                dhan = dhanhq(ctx)
                to_date = datetime.now().strftime("%Y-%m-%d")
                from_date = (datetime.now() - timedelta(days=2)).strftime("%Y-%m-%d")
                res_opt = dhan.intraday_minute_data(
                    security_id=opt_info['security_id'],
                    exchange_segment=opt_info['exchange_segment'],
                    instrument_type="OPTIDX",
                    from_date=from_date,
                    to_date=to_date,
                    interval=1
                )
                if res_opt.get("status") == "success" and "data" in res_opt and len(res_opt["data"].get("close", [])) > 0:
                    target_ltp = float(res_opt["data"]["close"][-1])
                    recommended_entry = target_ltp # Buy at CMP
            except Exception as e:
                print(f"Failed to fetch option LTP: {e}")
    
    return {
        "status": "success",
        "symbol": req.symbol,
        "target_symbol": target_symbol,
        "target_ltp": target_ltp,
        "recommended_entry": recommended_entry,
        "confidence_score": float(prob * 100) if prob > 0.5 else float((1-prob) * 100),
        "signal": signal,
        "strategy": strategy_used,
        "sentiment_score": sentiment_score,
        "spot_price": float(df['Close'].iloc[-1]),
        "atr": float(df_features['ATR_14'].iloc[-1]) if 'ATR_14' in df_features else 30.0,
        "sma_10": float(df_features['SMA_10'].iloc[-1]) if 'SMA_10' in df_features else 0.0,
        "sma_20": float(df_features['SMA_20'].iloc[-1]) if 'SMA_20' in df_features else 0.0,
        "rsi_14": float(df_features['RSI_14'].iloc[-1]) if 'RSI_14' in df_features else 50.0
    }

class PredictCryptoRequest(BaseModel):
    symbol: str # BTCUSDT, ETHUSDT
    interval: str = "1m"

@app.post("/models/predict_crypto")
def predict_crypto(req: PredictCryptoRequest):
    req_symbol = req.symbol.upper()
    trend_model_path = os.path.join(MODELS_DIR, f"{req_symbol}_trend_xgb.joblib")
    meanrev_model_path = os.path.join(MODELS_DIR, f"{req_symbol}_meanrev_xgb.joblib")
    
    if not os.path.exists(trend_model_path) or not os.path.exists(meanrev_model_path):
        return {"status": "error", "message": "Models not found. Train first.", "confidence_score": 50.0}
        
    trend_model = joblib.load(trend_model_path)
    meanrev_model = joblib.load(meanrev_model_path)
    
    df = fetch_binance_data(req.symbol, "1d", req.interval)
    
    if df.empty:
        return {"status": "error", "message": "Binance API returned empty data.", "confidence_score": 0.0}
        
    df_features = compute_features(df)
    if df_features.empty:
        return {"status": "error", "message": "Not enough recent data for features", "confidence_score": 50.0}
        
    features = get_feature_columns()
    latest_features = df_features[features].iloc[-1:]
    
    prob_trend = trend_model.predict_proba(latest_features)[0][1]
    conf_trend = prob_trend if prob_trend > 0.5 else (1 - prob_trend)
    
    prob_meanrev = meanrev_model.predict_proba(latest_features)[0][1]
    conf_meanrev = prob_meanrev if prob_meanrev > 0.5 else (1 - prob_meanrev)
    
    if conf_trend >= conf_meanrev:
        prob = prob_trend
        strategy_used = "TREND_FOLLOWING"
    else:
        prob = prob_meanrev
        strategy_used = "MEAN_REVERSION"
    
    if prob > 0.55:
        signal = "BUY_LONG"
    elif prob < 0.45:
        signal = "SELL_SHORT"
    else:
        signal = "HOLD"

    spot = float(df['Close'].iloc[-1])
    
    return {
        "status": "success",
        "symbol": req.symbol,
        "target_symbol": req.symbol,
        "target_ltp": spot,
        "recommended_entry": spot,
        "confidence_score": float(prob * 100) if prob > 0.5 else float((1-prob) * 100),
        "signal": signal,
        "strategy": strategy_used,
        "spot_price": spot,
        "atr": float(df_features['ATR_14'].iloc[-1]) if 'ATR_14' in df_features else (spot * 0.005)
    }

class ManageTradeRequest(BaseModel):
    symbol: str # NIFTY, SENSEX
    target_option: str # The option being traded
    signal_type: str = "UNKNOWN"
    entry_price: float
    current_price: float
    stop_loss: float
    target: float
    quantity: int
    stop_loss_breached: bool = False
    client_id: Optional[str] = None
    access_token: Optional[str] = None

@app.post("/models/manage_trade")
def manage_trade(req: ManageTradeRequest):
    req_symbol = req.symbol.upper()
    
    action = "HOLD"
    explanation = "Position is within normal parameters."
    recovery_prob = 50.0
    
    profit_pts = req.current_price - req.entry_price
    target_pts = req.target - req.entry_price
    
    # 1. Fetch spot index data to check momentum
    spot_symbol = req.target_option.split("-")[0] if "-" in req.target_option else req_symbol
    try:
        df = fetch_dhan_data(req.client_id, req.access_token, spot_symbol, "1d", "1m")
        if not df.empty:
            df_features = compute_features(df)
            macd_hist = float(df_features['MACD_Hist'].iloc[-1]) if 'MACD_Hist' in df_features else 0.0
            
            # 2. Check for Momentum Reversal if in profit
            if profit_pts > (target_pts * 0.15):
                if req.signal_type == "BUY_CALL" and macd_hist < 0:
                    action = "EXIT"
                    explanation = "Momentum (MACD) reversed to bearish. Booking profit early."
                    return {"status": "success", "action": action, "recovery_probability": 0.0, "explanation": explanation}
                elif req.signal_type == "BUY_PUT" and macd_hist > 0:
                    action = "EXIT"
                    explanation = "Momentum (MACD) reversed to bullish. Booking profit early."
                    return {"status": "success", "action": action, "recovery_probability": 0.0, "explanation": explanation}
    except Exception as e:
        print(f"Error checking momentum in manage_trade: {e}")

    if req.stop_loss_breached:
        recovery_prob = 45.0
        action = "EXIT"
        explanation = f"Exiting position. Stop loss breached. Recovery probability is too low ({recovery_prob}%)."
    else:
        # Dynamic Trailing Checkpoints
        if profit_pts > (target_pts * 0.60):
            action = "MOVE_STOP_LOSS"
            explanation = "Price has reached 60% of target. Tightening stop loss."
        elif profit_pts > (target_pts * 0.40):
            action = "MOVE_STOP_LOSS"
            explanation = "Price has reached 40% of target. Tightening stop loss."
        elif profit_pts > (target_pts * 0.20):
            action = "MOVE_STOP_LOSS"
            explanation = "Price has reached 20% of target. Moving stop loss near entry."
            
    return {
        "status": "success",
        "action": action,
        "recovery_probability": float(recovery_prob),
        "explanation": explanation
    }
