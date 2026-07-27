import requests
import datetime

def test_sensex_data():
    url = "https://api.dhan.co/v2/charts/intraday"
    headers = {
        "client-id": "1112521202",
        "access-token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzg1MjExMDU4LCJpYXQiOjE3ODUxMjQ2NTgsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.mV884cC2kDUwit-49c42fdEZu-4YX4uzJpDdaB-tZvIPRbB6CP8KUM2zCeTWc2--hL8ikcjXhdq_UTU4jErGbQ",
        "Content-Type": "application/json"
    }
    payload = {
        "securityId": "51",
        "exchangeSegment": "IDX_I",
        "instrument": "INDEX",
        "interval": 1,
        "oi": False,
        "fromDate": "2026-07-12",
        "toDate": "2026-07-17"
    }
    res = requests.post(url, headers=headers, json=payload)
    if res.status_code == 200:
        data = res.json()
        timestamps = data.get('timestamp', [])
        print("Total candles for Sensex:", len(timestamps))
        if timestamps:
            print("First candle time:", datetime.datetime.fromtimestamp(timestamps[0]).strftime('%Y-%m-%d %H:%M:%S'))
            print("Last candle time:", datetime.datetime.fromtimestamp(timestamps[-1]).strftime('%Y-%m-%d %H:%M:%S'))
    else:
        print(res.status_code, res.text)

if __name__ == "__main__":
    test_sensex_data()
