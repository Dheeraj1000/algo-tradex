import sys
import os

# Ensure we can import from ai-service
sys.path.append(os.path.dirname(os.path.abspath(__file__)))

from main import background_train, TrainRequest
from db_utils import get_active_token
token = get_active_token()
if not token:
    token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzg1ODE1NTU1LCJpYXQiOjE3ODU3MjkxNTUsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.Aw9DPnpbGs1Q9ZxVcxgaO-qHY3z1yZFohkOhlyOhimAEiR3vLc1Ld_0kCZQARRnQ_UzxARyU0CVQ9XkZ7J8jyA"
cid = "1112521202"

print("--- TRAINING NIFTY (5-MIN INTERVAL) WITH CANDLESTICKS ---")
req_nifty = TrainRequest(symbol="NIFTY", period="5d", interval="5m", client_id=cid, access_token=token)
background_train(req_nifty)

print("\n--- TRAINING SENSEX (5-MIN INTERVAL) WITH CANDLESTICKS ---")
req_sensex = TrainRequest(symbol="SENSEX", period="5d", interval="5m", client_id=cid, access_token=token)
background_train(req_sensex)

print("\n--- TRAINING BANKNIFTY (5-MIN INTERVAL) WITH CANDLESTICKS ---")
req_banknifty = TrainRequest(symbol="BANKNIFTY", period="5d", interval="5m", client_id=cid, access_token=token)
background_train(req_banknifty)

print("\nAll models trained and saved to models/ directory successfully!")
