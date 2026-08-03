new_tok = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzg1ODQzMjEyLCJpYXQiOjE3ODU3NTY4MTIsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.ZFFAOIEyy7oxY2KYC4J8bTKEGVWpOsNcz7NR2eLiqJ_NodQjlYZv9LzyQTla7WZn0pSwlXb4SmTMBWaiTPStLg"
open(".dhan_token", "w").write(new_tok)
import subprocess
subprocess.run(["psql", "-U", "postgres", "-d", "algotradex", "-c", f"UPDATE broker_accounts SET access_token='{new_tok}';"])
