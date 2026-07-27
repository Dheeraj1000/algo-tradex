import requests

def test_market_data():
    url = "http://localhost:8080/api/market-data/candles?symbol=NIFTY"
    # Using the JWT token of user
    headers = {
        "Authorization": "Bearer eyJhbGciOiJIUzUxMiJ9.eyJ1bmFtZSI6IkRoZWVyYWoiLCJ1Y29kZSI6IjExMTI1MjEyMDIifQ.xxx"
    }
    # We can't use the fake token, we'll get 401. 
    # Let me bypass auth or just run a direct H2 script? No, wait!
    pass
