import requests
from datetime import datetime, timedelta

def test_dhan_hist(client_id, access_token):
    url = "https://api.dhan.co/v2/charts/intraday"
    headers = {
        "client-id": client_id,
        "access-token": access_token,
        "Content-Type": "application/json"
    }
    payload = {
        "securityId": "13",
        "exchangeSegment": "IDX_I",
        "instrument": "INDEX",
        "interval": 1,
        "oi": False,
        "fromDate": "2024-01-01",
        "toDate": "2024-01-05"
    }
    res = requests.post(url, headers=headers, json=payload)
    print(res.status_code)
    print(res.text[:500])

if __name__ == "__main__":
    test_dhan_hist("1112521202", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzgzNjE2MjUzLCJpYXQiOjE3ODM1Mjk4NTMsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.QLg93TpRlCln85e5c84KUCQJvs-1AZPHIZvEH-nPLdplG1atwe4saEgkJPoofkQh86fs71RbTljUyTDUc3za8g")
