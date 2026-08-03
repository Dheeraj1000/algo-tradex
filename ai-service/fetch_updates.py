import requests
import json

token = "8836474667:AAHlJiJDav0l0Uvk27mYMoQwcXFkLb5wehw"
url = f"https://api.telegram.org/bot{token}/getUpdates"
res = requests.get(url).json()

if res.get("ok"):
    for msg in res.get("result", []):
        if "message" in msg:
            chat = msg["message"]["chat"]
            title = chat.get("title", chat.get("first_name", "Unknown"))
            chat_id = chat.get("id")
            print(f"Found Chat: {title} -> ID: {chat_id}")
else:
    print(res)
