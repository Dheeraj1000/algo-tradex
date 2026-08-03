#!/bin/bash

# Start the background workers with an ampersand (&) so they run in the background
echo "Starting Token Refresher..."
python token_refresher.py &

echo "Starting Market Monitor..."
python market_monitor.py &

# Start the main web server in the foreground
echo "Starting Uvicorn AI API Server..."
uvicorn main:app --host 0.0.0.0 --port ${PORT:-8000}
