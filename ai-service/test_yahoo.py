import requests
import urllib.parse

def test_yahoo(symbol):
    encoded = urllib.parse.quote(symbol)
    url = f"https://query1.finance.yahoo.com/v8/finance/chart/{encoded}?interval=1m&range=1d"
    headers = {"User-Agent": "Mozilla/5.0"}
    res = requests.get(url, headers=headers)
    print(f"Status for {symbol}: {res.status_code}")
    if res.status_code == 200:
        data = res.json()
        result = data.get("chart", {}).get("result", [])
        if result:
            print(f"Got {len(result[0].get('timestamp', []))} candles")
        else:
            print("No result in chart")
    else:
        print(res.text)

test_yahoo("^NSEI")
test_yahoo("^BSESN")
test_yahoo("RELIANCE.NS")
