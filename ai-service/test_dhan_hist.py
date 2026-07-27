import requests
from datetime import datetime, timedelta

def test_dhan_hist(client_id, access_token):
    url = "https://api.dhan.co/charts/intraday"
    headers = {
        "client-id": client_id,
        "access-token": access_token,
        "Content-Type": "application/json"
    }
    # from dhanhq source code for intraday_minute_data
    payload = {
        "securityId": "13",
        "exchangeSegment": "IDX_I",
        "instrument": "INDEX",
        "fromDate": (datetime.now() - timedelta(days=2)).strftime("%Y-%m-%d"),
        "toDate": datetime.now().strftime("%Y-%m-%d")
    }
    res = requests.post(url, headers=headers, json=payload)
    print(res.status_code)
    print(res.text[:500])

if __name__ == "__main__":
    test_dhan_hist("1112521202", "eyJhbGciOiJIUzUxMiJ9.eyJ1bmFtZSI6IkRoZWVyYWoiLCJ1Y29kZSI6IjExMTI1MjEyMDIifQ.xxx") # Wait, I don't have the actual token right now.
