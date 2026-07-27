import requests

url = "http://127.0.0.1:8001/models/predict"
data = {
    "symbol": "NIFTY",
    "client_id": "1112521202",
    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzgzNzQ0OTQwLCJpYXQiOjE3ODM2NTg1NDAsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.dpsO4c1toTo58rF0LOG3KEZwYp3cUmYqyvPmfhVUA7Nn7qhxd09RkTZsyAWyZ2sQO6U9FMmiFhNxrudv3wJg4g"
}

try:
    response = requests.post(url, json=data)
    result = response.json()
    print("AI Engine Prediction for NIFTY:")
    print(f"Signal: {result.get('signal')}")
    print(f"Target Symbol: {result.get('target_symbol')}")
    print(f"Confidence: {result.get('confidence_score'):.2f}%")
except Exception as e:
    print(f"Failed to get prediction: {e}")
