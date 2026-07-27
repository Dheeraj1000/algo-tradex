import psycopg2
conn = psycopg2.connect("dbname='algotradex' user='algotradex' password='algotradex123' host='localhost'")
cursor = conn.cursor()
cursor.execute("UPDATE broker_accounts SET access_token='eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzg0ODY3Mzg0LCJpYXQiOjE3ODQ3ODA5ODQsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.pFpXLyBuRMXlLsSTVKRHc4ArZlU9TlIcWeeZz_MBRxjVpn6tYGUt9dhOeRV1CLu44Ntznq_XsOSMIoWFJslG7g' WHERE client_id='1112521202';")
conn.commit()
cursor.close()
conn.close()
print('Updated successfully')
