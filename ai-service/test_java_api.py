import requests
import datetime

def test_api():
    login_url = "http://localhost:8080/api/auth/login"
    login_payload = {
        "email": "testuser@example.com",
        "password": "password"
    }
    res = requests.post(login_url, json=login_payload)
    if res.status_code != 200:
        print("Login failed:", res.status_code, res.text)
        return
        
    token = res.json().get('token')
    
    url = "http://localhost:8080/api/market-data/candles?symbol=NIFTY&limit=400"
    headers = {
        "Authorization": f"Bearer {token}"
    }
    res = requests.get(url, headers=headers)
    if res.status_code == 200:
        data = res.json()
        print("Total candles from backend API:", len(data))
        if data:
            print("First candle time (Epoch):", data[0]['time'])
            print("Last candle time (Epoch):", data[-1]['time'])
            print("Last candle time (Local):", datetime.datetime.fromtimestamp(data[-1]['time']).strftime('%Y-%m-%d %H:%M:%S'))
            print("Is 11:08 anywhere near the end?")
            last_few = [datetime.datetime.fromtimestamp(c['time']).strftime('%Y-%m-%d %H:%M:%S') for c in data[-10:]]
            print("Last 10 candles:", last_few)
    else:
        print("Market data failed:", res.status_code, res.text)

if __name__ == "__main__":
    test_api()
