import requests
import datetime

def test_option_data():
    url = "https://api.dhan.co/v2/charts/intraday"
    headers = {
        "client-id": "1112521202",
        "access-token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzgzNjE2MjUzLCJpYXQiOjE3ODM1Mjk4NTMsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.QLg93TpRlCln85e5c84KUCQJvs-1AZPHIZvEH-nPLdplG1atwe4saEgkJPoofkQh86fs71RbTljUyTDUc3za8g",
        "Content-Type": "application/json"
    }
    payload = {
        "securityId": "48995",
        "exchangeSegment": "NSE_FNO",
        "instrument": "OPTIDX",
        "interval": 1,
        "oi": False,
        "fromDate": "2026-07-09",
        "toDate": "2026-07-09"
    }
    res = requests.post(url, headers=headers, json=payload)
    if res.status_code == 200:
        data = res.json()
        timestamps = data.get('timestamp', [])
        print("Total candles for option:", len(timestamps))
        if timestamps:
            print("First candle time:", datetime.datetime.fromtimestamp(timestamps[0]).strftime('%Y-%m-%d %H:%M:%S'))
            print("Last candle time:", datetime.datetime.fromtimestamp(timestamps[-1]).strftime('%Y-%m-%d %H:%M:%S'))
    else:
        print(res.status_code, res.text)

if __name__ == "__main__":
    test_option_data()
