import pandas as pd
import os
from datetime import datetime
from dhanhq import dhanhq, DhanContext

csv_path = os.path.join(os.path.dirname(__file__), "data", "api-scrip-master.csv")
df = pd.read_csv(csv_path, low_memory=False)

# Filter for NIFTY options
opts = df[(df['SEM_CUSTOM_SYMBOL'].str.startswith('NIFTY')) & 
          (df['SEM_INSTRUMENT_NAME'] == 'OPTIDX')]

# Look for 23900 PE
pe_opts = opts[(opts['SEM_STRIKE_PRICE'] == 23900) & (opts['SEM_OPTION_TYPE'] == 'PE')]

# Find the one that matches 14 Jul (or nearest expiry)
print("Available 23900 PE Expiries:")
for _, row in pe_opts.sort_values('SEM_EXPIRY_DATE').head(5).iterrows():
    print(f"Symbol: {row['SEM_TRADING_SYMBOL']}, Expiry: {row['SEM_EXPIRY_DATE']}, Security ID: {row['SEM_SMST_SECURITY_ID']}")

# Just take the first one if we can't find exact 14 Jul, or search for 14 Jul
jul14_opt = pe_opts[pe_opts['SEM_TRADING_SYMBOL'].str.contains('Jul') | pe_opts['SEM_TRADING_SYMBOL'].str.contains('JUL')]
if jul14_opt.empty:
    target = pe_opts.sort_values('SEM_EXPIRY_DATE').iloc[0]
else:
    # try to get exactly 14 if exists
    exact = jul14_opt[jul14_opt['SEM_TRADING_SYMBOL'].str.contains('14')]
    if not exact.empty:
        target = exact.iloc[0]
    else:
        target = jul14_opt.iloc[0]

print(f"\nSelected Target: {target['SEM_TRADING_SYMBOL']} (ID: {target['SEM_SMST_SECURITY_ID']})")

client_id = "1112521202"
access_token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzgzNjE2MjUzLCJpYXQiOjE3ODM1Mjk4NTMsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.QLg93TpRlCln85e5c84KUCQJvs-1AZPHIZvEH-nPLdplG1atwe4saEgkJPoofkQh86fs71RbTljUyTDUc3za8g"

ctx = DhanContext(client_id, access_token)
dhan = dhanhq(ctx)

req_list = {
    "NSE_FNO": [str(target['SEM_SMST_SECURITY_ID'])]
}

print("\nFetching Live Price via marketfeed.ticker...")
try:
    res = dhan.marketfeed.ticker(req_list)
    print("Market Feed Response:", res)
except Exception as e:
    print("Market Feed Error:", e)

print("\nTrying historical minute data for today to get latest close...")
try:
    import datetime
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    res2 = dhan.intraday_minute_data(
        security_id=str(target['SEM_SMST_SECURITY_ID']),
        exchange_segment="NSE_FNO",
        instrument_type="OPTIDX",
        from_date=today,
        to_date=today,
        interval=1
    )
    if res2.get('status') == 'success' and 'data' in res2:
        closes = res2['data']['close']
        if closes:
            print(f"Latest Traded Price (LTP) from minute candles: {closes[-1]}")
        else:
            print("No trades today yet.")
    else:
        print("Minute data response:", res2)
except Exception as e:
    print("Minute data Error:", e)
