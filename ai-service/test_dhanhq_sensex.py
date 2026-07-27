from dhanhq import dhanhq, DhanContext
import datetime

client_id = "1112521202"
access_token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzg0MzQ3MTAxLCJpYXQiOjE3ODQyNjA3MDEsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.ToT4HqerncvlHdeTxYjEXsc6e3TRvgK3hLLpAHI9CwdWjmWVjzg-xrLCeIRt08k7lbqvWv6kSH5CsYIf_byqdw"

ctx = DhanContext(client_id, access_token)
dhan = dhanhq(ctx)

from_date = "2026-07-12"
to_date = "2026-07-17"
res = dhan.intraday_minute_data("51", "IDX_I", "INDEX", "1", from_date, to_date)
print("SENSEX DATA:")
print(res.keys() if isinstance(res, dict) else res)
print("status:", res.get("status") if isinstance(res, dict) else "")
