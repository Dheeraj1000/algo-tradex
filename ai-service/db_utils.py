import os
import psycopg2

DB_HOST = os.getenv("PGHOST", os.getenv("DB_HOST", "localhost"))
DB_PORT = os.getenv("PGPORT", "5432")
DB_NAME = os.getenv("PGDATABASE", "algotradex")
DB_USER = os.getenv("PGUSER", "algotradex")
DB_PASS = os.getenv("PGPASSWORD", "algotradex123")

def get_active_token():
    """
    Connects to the PostgreSQL database and retrieves the latest Dhan access token.
    """
    token = None
    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            port=DB_PORT,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASS
        )
        cur = conn.cursor()
        cur.execute("SELECT access_token FROM broker_accounts WHERE broker_type = 'DHAN' LIMIT 1;")
        result = cur.fetchone()
        if result:
            token = result[0]
        cur.close()
        conn.close()
    except Exception as e:
        print(f"Error fetching token from DB: {e}")
    return token

def update_active_token(new_token):
    """
    Updates the Dhan access token in the PostgreSQL database.
    """
    try:
        conn = psycopg2.connect(
            host=DB_HOST,
            port=DB_PORT,
            database=DB_NAME,
            user=DB_USER,
            password=DB_PASS
        )
        cur = conn.cursor()
        cur.execute("UPDATE broker_accounts SET access_token = %s WHERE broker_type = 'DHAN';", (new_token,))
        conn.commit()
        cur.close()
        conn.close()
        print("Successfully updated token in PostgreSQL.")
        return True
    except Exception as e:
        print(f"Failed to update PostgreSQL: {e}")
        return False
