import time
import requests
from db_utils import get_active_token, update_active_token

CLIENT_ID = "1112521202"
RENEW_URL = "https://api.dhan.co/v2/RenewToken"

def renew_token():
    current_token = get_active_token()
    if not current_token:
        print("No current token found in database to renew!")
        return False
        
    headers = {
        "access-token": current_token,
        "dhanClientId": CLIENT_ID,
        "Accept": "application/json"
    }
    
    try:
        res = requests.get(RENEW_URL, headers=headers, timeout=15)
        if res.status_code == 200:
            data = res.json()
            new_token = data.get("token")
            if new_token:
                update_active_token(new_token)
                print(f"Successfully renewed token at {time.ctime()}")
                return True
        else:
            print(f"Failed to renew token. Status Code: {res.status_code}")
            print(res.text)
    except Exception as e:
        print(f"Error renewing token: {e}")
        
    return False

if __name__ == "__main__":
    print("Starting stateless automated Dhan token refresher (runs every 12 hours)...")
    while True:
        time.sleep(12 * 60 * 60)
        renew_token()
