import os
import pandas as pd
import requests
import datetime
from io import BytesIO

CSV_URL = "https://images.dhan.co/api-data/api-scrip-master.csv"
LOCAL_CSV = os.path.join(os.path.dirname(__file__), "api-scrip-master.csv")

def download_master_csv(force=False):
    # Only download if it doesn't exist or if forced (or if we wanted to check date)
    if not force and os.path.exists(LOCAL_CSV):
        # Check if downloaded today
        mod_time = datetime.datetime.fromtimestamp(os.path.getmtime(LOCAL_CSV))
        if mod_time.date() == datetime.date.today():
            print("Already have today's Dhan scrip master.")
            return

    print("Downloading Dhan api-scrip-master.csv...")
    try:
        response = requests.get(CSV_URL)
        response.raise_for_status()
        with open(LOCAL_CSV, 'wb') as f:
            f.write(response.content)
        print("Download complete.")
    except Exception as e:
        print(f"Failed to download scrip master: {e}")

def get_atm_option(symbol: str, spot_price: float, option_type: str = "CE"):
    """
    symbol: e.g., 'NIFTY' or 'BANKNIFTY'
    spot_price: e.g., 24350.5
    option_type: 'CE' or 'PE'
    Returns: a dict with the security_id, trading_symbol, etc.
    """
    if not os.path.exists(LOCAL_CSV):
        download_master_csv()

    try:
        # Load CSV (low_memory=False because it has many columns and mixed types)
        df = pd.read_csv(LOCAL_CSV, low_memory=False)
        
        # Filter for NSE/BSE options
        df_fno = df[(df['SEM_EXM_EXCH_ID'].isin(['NSE', 'BSE'])) & 
                    (df['SEM_INSTRUMENT_NAME'] == 'OPTIDX')]
        
        # Strip prefixes if any. E.g. ^NSEI -> NIFTY
        clean_symbol = symbol.replace('^', '').replace('NSEI', 'NIFTY').replace('BSESN', 'SENSEX').upper()
        
        # We can filter by SEM_TRADING_SYMBOL starting with NIFTY or SENSEX
        df_symbol = df_fno[df_fno['SEM_TRADING_SYMBOL'].str.startswith(clean_symbol, na=False)]
        
        if df_symbol.empty:
            print(f"No options found for symbol {clean_symbol}")
            return None
            
        # Ensure Expiry Date is in datetime
        df_symbol = df_symbol.copy()
        df_symbol['EXPIRY'] = pd.to_datetime(df_symbol['SEM_EXPIRY_DATE'])
        
        # Filter out expired options (expiry < today)
        df_active = df_symbol[df_symbol['EXPIRY'].dt.date >= datetime.date.today()]
        
        if df_active.empty:
            print(f"No active options found for symbol {clean_symbol}")
            return None
            
        # Get nearest expiry
        nearest_expiry = df_active['EXPIRY'].min()
        df_nearest = df_active[df_active['EXPIRY'] == nearest_expiry]
        
        # Filter by CE/PE
        df_nearest_type = df_nearest[df_nearest['SEM_OPTION_TYPE'] == option_type.upper()].copy()
        
        # Find closest strike to spot price
        df_nearest_type['STRIKE_DIFF'] = abs(df_nearest_type['SEM_STRIKE_PRICE'] - spot_price)
        atm_row = df_nearest_type.loc[df_nearest_type['STRIKE_DIFF'].idxmin()]
        
        exch_id = atm_row['SEM_EXM_EXCH_ID']
        exchange_segment = 'BSE_FNO' if exch_id == 'BSE' else 'NSE_FNO'
        
        return {
            'security_id': str(atm_row['SEM_SMST_SECURITY_ID']),
            'trading_symbol': atm_row['SEM_TRADING_SYMBOL'],
            'strike_price': float(atm_row['SEM_STRIKE_PRICE']),
            'expiry': atm_row['SEM_EXPIRY_DATE'],
            'exchange_segment': exchange_segment
        }
    except Exception as e:
        print(f"Error parsing Dhan scrip master: {e}")
        return None

if __name__ == "__main__":
    download_master_csv(force=True)
