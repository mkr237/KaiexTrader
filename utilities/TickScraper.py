import websocket
import json
from datetime import datetime, timezone
from threading import Thread

# Script for pulling DYDX tick data into CSV files - files (one for each pair) are
# rolled over at midnight UTC each day

CURRENCY_PAIRS = ["ETH-USD"]  # Add more pairs as needed"BTC-USD",
files = {}
current_file_days = {}

def create_file_name(pair):
    """Create a filename based on the current date and currency pair."""
    return f"{pair}_{datetime.now(timezone.utc).strftime('%Y%m%d')}.dat"

def on_message(ws, pair, message):
    global files, current_file_days
    message_dict = json.loads(message)

    # Check if the message has the needed data and the type is 'channel_data'
    #
    if message_dict.get('type') == 'channel_data' and'contents' in message_dict and 'trades' in message_dict['contents']:
        print(f"{datetime.now(timezone.utc)}: Received {len(message_dict['contents']['trades'])} new trade(s) for {pair}")
        for trade in message_dict['contents']['trades']:
            # Parse the 'createdAt' timestamp to get the day
            trade_day = datetime.fromisoformat(trade['createdAt'].replace('Z', '+00:00')).date()

            # If this is the first message or the day has changed, roll over to a new file
            if current_file_days.get(pair) is None or trade_day != current_file_days[pair]:
                if files.get(pair) is not None:
                    files[pair].close()

                filename = create_file_name(pair)
                print(f"Creating new file: {filename}")
                files[pair] = open(filename, 'w')
                current_file_days[pair] = trade_day

            # Write the data to the file and immediately
            files[pair].write(f"{trade['createdAt']},{trade['side']},{trade['size']},{trade['price']},{trade['liquidation']}\n")

        # flush the buffer to disk
        files[pair].flush()

def on_error(ws, error):
    print(error)

def on_close(ws):
    print("### closed ###")

def on_open(ws, pair):
    def run(*args):
        subscribe_message = {
            "type": "subscribe",
            "channel": "v3_trades",
            "id": pair
        }
        ws.send(json.dumps(subscribe_message))

    Thread(target=run).start()

def create_ws(pair):
    ws = websocket.WebSocketApp(
        "wss://api.dydx.exchange/v3/ws",
        on_message=lambda ws, message: on_message(ws, pair, message),
        on_error=on_error,
        on_close=on_close)
    ws.on_open = lambda ws: on_open(ws, pair)
    return ws

if __name__ == "__main__":
    # websocket.enableTrace(True)
    for pair in CURRENCY_PAIRS:
        print(f"{datetime.now(timezone.utc)}: Subscribing to {pair}")
        ws = create_ws(pair)
        Thread(target=ws.run_forever).start()
