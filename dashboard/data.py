import pandas as pd
import requests


#
# Get trading metrics
#
def getTradingMetrics():

    # Make a GET request to the API endpoint
    response = requests.get('http://localhost:8080/api/metrics')

    # Check if the request was successful (status code 200)
    if response.status_code == 200:
        data = response.json()
        return {
            'pnl': data['pnl'],
            'numTrades': data['numTrades'],
            'winRate': data['winRate'],
            'sharpe': data['sharpe']
        }

    return {
        'pnl': 0,
        'numTrades': 0,
        'winRate': 0,
        'sharpe': 0
    }

#
# Get market data
#
def getChartData():

    # Make a GET request to the API endpoint
    response = requests.get('http://localhost:8080/api/chart')

    # Check if the request was successful (status code 200)
    if response.status_code == 200:
        return response.json()

    else:
        return ""

#
# Get order data
#
def getOrderData():

    # Make GET requests to the API endpoints for orders and fills
    orders_response = requests.get('http://localhost:8080/api/orders')

    # Check if the requests were successful (status code 200)
    if orders_response.status_code == 200:

        orders_data = orders_response.json()

        # Convert response data to a tabular format
        table_data = []
        for order_id, order_data in sorted(orders_data.items(), key=lambda x: x[1]["createdAt"]):
            order_row = {
                "ID": "..." + order_data["orderId"][-3:],
                "symbol": order_data["symbol"],
                "type": order_data["type"],
                "side": order_data["side"],
                "price": order_data["price"],
                "size": order_data["size"],
                "remaining": order_data["remainingSize"],
                "status": order_data["status"],
                "TIF": order_data["timeInForce"],
                "createdAt": pd.to_datetime(order_data["createdAt"], unit="ms"),
                "expiresAt": pd.to_datetime(order_data["expiresAt"], unit="ms"),
            }
            table_data.append(order_row)
            fills = order_data.get("fills", [])
            for fill in fills:
                fill_row = {
                    "ID": "..." + fill["fillId"][-3:],
                    "price": fill["price"],
                    "size": fill["size"],
                    "createdAt": pd.to_datetime(fill["createdAt"], unit="ms"),
                    "updatedAt": pd.to_datetime(fill["updatedAt"], unit="ms"),
                }
                table_data.append(fill_row)

        # Create a DataFrame from the tabular data
        orders_df = pd.DataFrame(table_data)

        # Apply row-level styling
        styled_df = orders_df.style.apply(lambda x: ['background-color: #EEEEEE' if x['symbol'] == 'BTC-USD' else '' for i in x], axis=1)
        return styled_df

    else:
        return pd.DataFrame.empty

def getPositionsData():

    # Make GET requests to the API endpoints for orders and fills
    positions_response = requests.get('http://localhost:8080/api/positions')

    # Check if the requests were successful (status code 200)
    if positions_response.status_code == 200:

        positions_data = positions_response.json()

        table_data = []
        for symbol, positions in positions_data.items():
            for position_id, position in positions.items():
                position["symbol"] = symbol
                table_data.append(position)

        # Create a DataFrame from the list of dictionaries
        positions_df = pd.DataFrame(table_data)

        # Convert the timestamp columns to datetime
        date_columns = ["closedAt", "updatedAt", "createdAt"]
        positions_df[date_columns] = positions_df[date_columns].apply(pd.to_datetime, unit="ms")

        subset_df = positions_df.loc[:, ['positionId', 'symbol', 'side', 'status', 'size', 'entryPrice', 'exitPrice', 'realizedPnl']]

        # Apply row-level styling
        styled_df = subset_df.style.apply(lambda x: ['background-color: #9FD8CE' if x['realizedPnl'] >= 0 else 'background-color: #FF9C99' for i in x], axis=1)
        return styled_df

    else:
        return pd.DataFrame.empty