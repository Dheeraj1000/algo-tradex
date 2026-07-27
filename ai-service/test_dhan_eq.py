import requests

def test_dhan_hist():
    url = "https://api.dhan.co/v2/charts/intraday"
    headers = {
        "client-id": "1112521202",
        "access-token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzgzNjE2MjUzLCJpYXQiOjE3ODM1Mjk4NTMsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.QLg93TpRlCln85e5c84KUCQJvs-1AZPHIZvEH-nPLdplG1atwe4saEgkJPoofkQh86fs71RbTljUyTDUc3za8g",
        "Content-Type": "application/json"
    }
    payload = {
        "securityId": "13",
        "exchangeSegment": "NSE_EQ",
        "instrument": "EQUITY",
        "interval": 1,
        "oi": False,
        "fromDate": "2024-07-09",
        "toDate": "2024-07-09"
    }
    res = requests.post(url, headers=headers, json=payload)
    if res.status_code == 200:
        data = res.json()
        print("Prices:", data.get('close', [])[:5])
    else:
        print(res.status_code, res.text)

if __name__ == "__main__":
    test_dhan_hist()
