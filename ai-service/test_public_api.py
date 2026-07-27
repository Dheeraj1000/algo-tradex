import requests
import datetime

def test_public_api():
    url = "http://localhost:8080/api/market-data/public-candles?symbol=NIFTY&limit=400"
    res = requests.get(url)
    if res.status_code == 200:
        data = res.json()
        print("Total candles from public API:", len(data))
        if data:
            print("First candle time (Epoch):", data[0]['time'])
            print("Last candle time (Epoch):", data[-1]['time'])
            print("Last candle time (Local):", datetime.datetime.fromtimestamp(data[-1]['time']).strftime('%Y-%m-%d %H:%M:%S'))
    else:
        print("Market data failed:", res.status_code, res.text)

if __name__ == "__main__":
    test_public_api()
