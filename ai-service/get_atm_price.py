import yfinance as yf
from data.dhan_master_downloader import get_atm_option
from dhanhq import dhanhq, DhanContext
import datetime

client_id = "1112521202"
access_token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzgzNjE2MjUzLCJpYXQiOjE3ODM1Mjk4NTMsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.QLg93TpRlCln85e5c84KUCQJvs-1AZPHIZvEH-nPLdplG1atwe4saEgkJPoofkQh86fs71RbTljUyTDUc3za8g"

print("Fetching Spot Price for NIFTY...")
spot = yf.Ticker("^NSEI").history(period="1d", interval="1m")['Close'].iloc[-1]
print(f"Spot Price: {spot}")

print("\nFinding ATM CE Option...")
opt_info = get_atm_option("NIFTY", spot, "CE")
print(f"Target Option: {opt_info['trading_symbol']}")
print(f"Security ID: {opt_info['security_id']}")

ctx = DhanContext(client_id, access_token)
dhan = dhanhq(ctx)

print("\nFetching latest price from Dhan...")
today = datetime.datetime.now().strftime("%Y-%m-%d")
res = dhan.intraday_minute_data(
    security_id=opt_info['security_id'],
    exchange_segment=opt_info['exchange_segment'],
    instrument_type="OPTIDX",
    from_date=today,
    to_date=today,
    interval=1
)

if res.get('status') == 'success' and 'data' in res:
    closes = res['data']['close']
    if closes:
        print(f"\n=> Current Live Price of {opt_info['trading_symbol']}: INR {closes[-1]}")
    else:
        print("No trades today yet.")
else:
    print("Error fetching minute data:", res)
