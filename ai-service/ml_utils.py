import pandas as pd
import numpy as np

def compute_rsi(data: pd.Series, window: int = 14) -> pd.Series:
    delta = data.diff()
    up, down = delta.copy(), delta.copy()
    up[up < 0] = 0
    down[down > 0] = 0
    
    roll_up = up.ewm(span=window).mean()
    roll_down = down.abs().ewm(span=window).mean()
    
    rs = roll_up / roll_down
    rsi = 100.0 - (100.0 / (1.0 + rs))
    return rsi

def compute_atr(df: pd.DataFrame, window: int = 14) -> pd.Series:
    high_low = df['High'] - df['Low']
    high_close = np.abs(df['High'] - df['Close'].shift())
    low_close = np.abs(df['Low'] - df['Close'].shift())
    ranges = pd.concat([high_low, high_close, low_close], axis=1)
    true_range = np.max(ranges, axis=1)
    return true_range.rolling(window).mean()

def compute_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    Computes advanced technical indicators as features for the ML model.
    """
    df = df.copy()
    
    # 1. Basic price features & Lags
    df['Returns'] = df['Close'].pct_change()
    df['Ret_Lag1'] = df['Returns'].shift(1)
    df['Ret_Lag2'] = df['Returns'].shift(2)
    df['Ret_Lag3'] = df['Returns'].shift(3)
    
    # 2. Moving Averages
    df['SMA_10'] = df['Close'].rolling(window=10).mean()
    df['SMA_20'] = df['Close'].rolling(window=20).mean()
    df['SMA_50'] = df['Close'].rolling(window=50).mean()
    
    df['Dist_SMA_10'] = (df['Close'] - df['SMA_10']) / df['SMA_10']
    df['Dist_SMA_20'] = (df['Close'] - df['SMA_20']) / df['SMA_20']
    
    # 3. RSI
    df['RSI_14'] = compute_rsi(df['Close'], 14)
    
    # 4. MACD
    ema_12 = df['Close'].ewm(span=12, adjust=False).mean()
    ema_26 = df['Close'].ewm(span=26, adjust=False).mean()
    df['MACD'] = ema_12 - ema_26
    df['MACD_Signal'] = df['MACD'].ewm(span=9, adjust=False).mean()
    df['MACD_Hist'] = df['MACD'] - df['MACD_Signal']
    
    # 5. Volatility & Bollinger Bands
    df['Volatility_10'] = df['Returns'].rolling(window=10).std()
    df['BB_Mid'] = df['SMA_20']
    df['BB_Upper'] = df['BB_Mid'] + 2 * df['Close'].rolling(window=20).std()
    df['BB_Lower'] = df['BB_Mid'] - 2 * df['Close'].rolling(window=20).std()
    df['BB_Width'] = (df['BB_Upper'] - df['BB_Lower']) / df['BB_Mid']
    df['BB_Pct'] = (df['Close'] - df['BB_Lower']) / (df['BB_Upper'] - df['BB_Lower'])
    
    # 6. Average True Range (ATR)
    df['ATR_14'] = compute_atr(df, 14)
    df['ATR_Ratio'] = df['ATR_14'] / df['Close']
    
    # 7. Stochastic Oscillator
    low_14 = df['Low'].rolling(window=14).min()
    high_14 = df['High'].rolling(window=14).max()
    df['Stoch_K'] = 100 * ((df['Close'] - low_14) / (high_14 - low_14))
    df['Stoch_D'] = df['Stoch_K'].rolling(window=3).mean()
    
    # 8. On-Balance Volume (OBV)
    obv = (np.sign(df['Returns']) * df['Volume']).fillna(0).cumsum()
    df['OBV'] = obv
    df['OBV_EMA'] = df['OBV'].ewm(span=20).mean()
    df['OBV_Dist'] = (df['OBV'] - df['OBV_EMA']) / df['OBV_EMA'].replace(0, 1)

    # 8.5 Option Chain Sentiment (OI & PCR)
    if 'CE_OI' in df.columns and 'PE_OI' in df.columns:
        df['PCR'] = df['PE_OI'] / df['CE_OI'].replace(0, 1)
        df['Call_OI_Change'] = df['CE_OI'].pct_change(fill_method=None)
        df['Put_OI_Change'] = df['PE_OI'].pct_change(fill_method=None)
    else:
        df['PCR'] = 1.0
        df['Call_OI_Change'] = 0.0
        df['Put_OI_Change'] = 0.0

    # 9a. Target Variable: Trend Following (Macro Momentum over next 2 Hours / 24 candles)
    next_return_trend = df['Close'].shift(-24) / df['Close'] - 1.0
    df['Target_Trend'] = (next_return_trend > 0.0002).astype(int)
    
    # 9b. Target Variable: Mean Reversion (Micro Pullbacks over next 15 mins / 3 candles)
    next_return_meanrev = df['Close'].shift(-3) / df['Close'] - 1.0
    df['Target_MeanRev'] = (next_return_meanrev > 0.0001).astype(int)
    
    # Replace infinities with NaN
    df.replace([np.inf, -np.inf], np.nan, inplace=True)
    feature_cols = [c for c in df.columns if not c.startswith('Target')]
    
    # Forward fill missing values to prevent dropping critical live rows!
    df[feature_cols] = df[feature_cols].ffill()
    
    # Fill any remaining NaNs (e.g. at the very start of the dataset) with 0
    df[feature_cols] = df[feature_cols].fillna(0)
    
    df.dropna(subset=feature_cols, inplace=True)
    
    return df

def get_feature_columns():
    return [
        'Returns', 'Ret_Lag1', 'Ret_Lag2', 'Ret_Lag3',
        'Dist_SMA_10', 'Dist_SMA_20', 'RSI_14', 
        'MACD', 'MACD_Signal', 'MACD_Hist', 'Volatility_10',
        'BB_Width', 'BB_Pct', 'ATR_Ratio', 'Stoch_K', 'Stoch_D', 'OBV_Dist',
        'PCR', 'Call_OI_Change', 'Put_OI_Change'
    ]
