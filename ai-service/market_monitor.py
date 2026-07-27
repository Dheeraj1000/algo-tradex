import time
import requests
import sys

def run_monitor():
    url = "http://127.0.0.1:8001/models/predict"
    token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzg1MjExMDU4LCJpYXQiOjE3ODUxMjQ2NTgsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.mV884cC2kDUwit-49c42fdEZu-4YX4uzJpDdaB-tZvIPRbB6CP8KUM2zCeTWc2--hL8ikcjXhdq_UTU4jErGbQ"
    client_id = "1112521202"
    
    last_alert = {"NIFTY": 0, "SENSEX": 0}
    
    print("Started monitoring NIFTY and SENSEX. Pinging AI Engine...")
    sys.stdout.flush()
    
    while True:
        try:
            for sym in ["NIFTY", "SENSEX"]:
                data = {"symbol": sym, "client_id": client_id, "access_token": token}
                res = requests.post(url, json=data).json()
                
                if res.get("status") == "success":
                    conf = res.get("confidence_score", 0)
                    sig = res.get("signal")
                    target = res.get("target_symbol")
                    
                    sma_10 = res.get("sma_10", 0)
                    sma_20 = res.get("sma_20", 0)
                    rsi = res.get("rsi_14", 50)
                    spot = res.get("spot_price", 0)
                    
                    # Chop Filter: If SMA-10 and SMA-20 are too close (< 0.05%), the market is flat/choppy
                    is_choppy = abs(sma_10 - sma_20) < (spot * 0.0005)
                    
                    # Confluence Logic: Combine AI Confidence with Technical Indicators
                    confluence = False
                    if sig == "BUY_CALL" and conf >= 70.0:
                        # Ensure Spot is above SMA (Uptrend), RSI is not severely overbought, and market is NOT choppy
                        if spot > sma_10 and rsi < 75 and not is_choppy:
                            confluence = True
                        else:
                            print(f"DEBUG: Skipping BUY_CALL - Chop/Trend Issue. Spot: {spot:.2f}, SMA10: {sma_10:.2f}, SMA20: {sma_20:.2f}, RSI: {rsi:.2f}")
                    elif sig == "BUY_PUT" and conf >= 70.0:
                        # Ensure Spot is below SMA (Downtrend), RSI is not severely oversold, and market is NOT choppy
                        if spot < sma_10 and rsi > 25 and not is_choppy:
                            confluence = True
                        else:
                            print(f"DEBUG: Skipping BUY_PUT - Chop/Trend Issue. Spot: {spot:.2f}, SMA10: {sma_10:.2f}, SMA20: {sma_20:.2f}, RSI: {rsi:.2f}")
                            
                    sentiment = res.get("sentiment_score", 0.0)
                    print(f"DEBUG: sym={sym} sig={sig} conf={conf} sentiment={sentiment} confluence={confluence}")
                    sys.stdout.flush()

                    if confluence and sig != "HOLD":
                        if time.time() - last_alert[sym] > 600: # 10-minute cooldown to prevent multiple entries
                            spot = res.get("spot_price", 0)
                            atr = res.get("atr", 30)
                            
                            target_ltp = res.get("target_ltp", 0.0)
                            recommended_entry = res.get("recommended_entry", 0.0)
                            
                            # Dynamic Targets based on Confidence
                            target_pct = 0.60 if conf >= 90.0 else (0.40 if conf >= 80.0 else 0.20)
                            if is_choppy: target_pct = max(0.10, target_pct - 0.10)
                            trail_pct = max(0.15, target_pct / 2.0)
                            
                            if recommended_entry > 0:
                                sl_pts = int(recommended_entry * 0.30) # Fixed 30% stop loss
                                trail_pts = int(recommended_entry * trail_pct)
                                target_pts = int(recommended_entry * target_pct)
                            else:
                                sl_pts = int(atr * 0.33)
                                trail_pts = int(atr * trail_pct)
                                target_pts = int(atr * target_pct)
                            
                            strategy = res.get("strategy", "UNKNOWN")
                            print(f"\n[ALERT] HIGH CONVICTION SETUP FOUND!")
                            print(f"Index: {sym} @ {spot:.2f}")
                            print(f"Target Option: {target}")
                            if target_ltp > 0:
                                print(f"  -> Current Premium: Rs.{target_ltp:.2f}")
                                print(f"  -> Entry Price: Rs.{recommended_entry:.2f}")
                            print(f"Signal: {sig} | Strategy: {strategy} | Confidence: {conf:.2f}%")
                            print(f"--> RECOMMENDED RISK/REWARD (30% Risk):")
                            print(f"    Trailing Trigger (+{int(trail_pct*100)}%): Rs.{recommended_entry + trail_pts:.2f} (Move SL to Entry!)")
                            print(f"    Target 1 (+{int(target_pct*100)}%): Rs.{recommended_entry + target_pts:.2f} (Sell 1 Lot)")
                            print(f"    Stop Loss (-30%): Rs.{recommended_entry - sl_pts:.2f} (Hard Exit)")
                            sys.stdout.flush()
                            
                            try:
                                payload = {
                                    "symbol": sym,
                                    "spotPrice": spot,
                                    "targetOption": target,
                                    "signal": sig,
                                    "confidence": conf,
                                    "targetProfit": target_pts,
                                    "stopLoss": sl_pts,
                                    "targetLtp": target_ltp,
                                    "recommendedEntry": recommended_entry
                                }
                                requests.post("http://127.0.0.1:8080/api/market-data/ai-alerts", json=payload, timeout=10)
                            except Exception as e:
                                print(f"Warning: Failed to broadcast alert to backend: {e}")
                                
                            last_alert[sym] = time.time()
                    # Removed the 'Scanning...' else block to prevent log spam
        except Exception as e:
            print(f"Error in monitor loop: {e}")
        time.sleep(5)

if __name__ == "__main__":
    run_monitor()
