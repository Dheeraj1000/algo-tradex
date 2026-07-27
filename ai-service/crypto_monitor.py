import time
import requests
from datetime import datetime
import json
import os

API_URL = "http://127.0.0.1:8001"
BACKEND_URL = "http://127.0.0.1:8080/api/market-data/ai-alerts"

def check_crypto_signals(symbol="BTCUSDT"):
    try:
        print(f"\n[{datetime.now().strftime('%H:%M:%S')}] Polling AI for {symbol}...")
        
        req_data = {
            "symbol": symbol,
            "interval": "1m"
        }
        
        res = requests.post(f"{API_URL}/models/predict_crypto", json=req_data)
        if res.status_code == 200:
            data = res.json()
            if data.get("status") == "success":
                signal = data.get("signal")
                spot = data.get("spot_price")
                conf = data.get("confidence_score", 0)
                strategy = data.get("strategy", "UNKNOWN")
                
                print(f"[{symbol}] Spot: ${spot:.2f} | Signal: {signal} ({conf:.1f}%) | Strat: {strategy}")
                
                if signal != "HOLD" and conf >= 75.0:
                    atr = data.get("atr", spot * 0.005) # Default 0.5% if ATR fails
                    
                    # Target 1% up/down, SL 0.5% up/down
                    if signal == "BUY_LONG":
                        sl = spot - atr
                        tp = spot + (atr * 2)
                    else: # SELL_SHORT
                        sl = spot + atr
                        tp = spot - (atr * 2)
                        
                    print("="*50)
                    print(f"🚀 CRYPTO TRADE ALERT: {signal} {symbol}")
                    print(f"Entry: ${spot:.2f}")
                    print(f"Target: ${tp:.2f} (1:2 Risk/Reward)")
                    print(f"Stop Loss: ${sl:.2f}")
                    print(f"Trailing Step: ${atr:.2f}")
                    print("="*50)
                    
                    try:
                        payload = {
                            "symbol": symbol,
                            "spotPrice": spot,
                            "targetOption": symbol,
                            "signal": signal,
                            "confidence": conf,
                            "targetProfit": tp,
                            "stopLoss": sl,
                            "targetLtp": spot,
                            "recommendedEntry": spot
                        }
                        requests.post(BACKEND_URL, json=payload, timeout=2)
                    except Exception as e:
                        print(f"Warning: Failed to broadcast alert to backend: {e}")
            else:
                print(f"[{symbol}] AI Error: {data.get('message')}")
        else:
            print(f"[{symbol}] API HTTP Error: {res.status_code}")
    except Exception as e:
        print(f"[{symbol}] Error polling API: {e}")

def main():
    symbols = ["BTCUSDT", "ETHUSDT"]
    
    print("Starting Crypto Market Monitor...")
    print("Press Ctrl+C to stop")
    
    while True:
        for sym in symbols:
            check_crypto_signals(sym)
            
        print("\nWaiting 60 seconds for next candle...")
        time.sleep(60)

if __name__ == "__main__":
    main()
