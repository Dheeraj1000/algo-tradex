import requests

def test_dhan_hist_range():
    url = "https://api.dhan.co/v2/charts/intraday"
    headers = {
        "client-id": "1112521202",
        "access-token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzgzNjE2MjUzLCJpYXQiOjE3ODM1Mjk4NTMsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.QLg93TpRlCln85e5c84KUCQJvs-1AZPHIZvEH-nPLdplG1atwe4saEgkJPoofkQh86fs71RbTljUyTDUc3za8g",
        "Content-Type": "application/json"
    }
    payload = {
        "securityId": "13",
        "exchangeSegment": "IDX_I",
        "instrument": "INDEX",
        "interval": 1,
        "oi": False,
        "fromDate": "2024-07-04",
        "toDate": "2024-07-09"
    }
    res = requests.post(url, headers=headers, json=payload)
    if res.status_code == 200:
        data = res.json()
        print("Total timestamps:", len(data.get('timestamp', [])))
        if data.get('timestamp'):
            import datetime
            print("First date:", datetime.datetime.fromtimestamp(data['timestamp'][0]).strftime('%Y-%m-%d %H:%M:%S'))
            print("Last date:", datetime.datetime.fromtimestamp(data['timestamp'][-1]).strftime('%Y-%m-%d %H:%M:%S'))
    else:
        print(res.status_code, res.text)

if __name__ == "__main__":
    test_dhan_hist_range()
