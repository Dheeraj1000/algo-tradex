import requests
import time

url_train = "http://127.0.0.1:8001/models/train"
url_predict = "http://127.0.0.1:8001/models/predict"
data = {
    "symbol": "NIFTY",
    "client_id": "1112521202",
    "access_token": "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzUxMiJ9.eyJpc3MiOiJkaGFuIiwicGFydG5lcklkIjoiIiwiZXhwIjoxNzgzNjE2MjUzLCJpYXQiOjE3ODM1Mjk4NTMsInRva2VuQ29uc3VtZXJUeXBlIjoiU0VMRiIsIndlYmhvb2tVcmwiOiIiLCJkaGFuQ2xpZW50SWQiOiIxMTEyNTIxMjAyIn0.QLg93TpRlCln85e5c84KUCQJvs-1AZPHIZvEH-nPLdplG1atwe4saEgkJPoofkQh86fs71RbTljUyTDUc3za8g"
}

print("Initiating Spot Training...")
res = requests.post(url_train, json=data)
print(res.json())

print("Waiting 10s for training to complete...")
time.sleep(10)

print("Predicting Spot Direction...")
res2 = requests.post(url_predict, json=data)
print(res2.json())
