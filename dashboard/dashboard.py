import streamlit as st
import pandas as pd
import plotly.graph_objects as go
from plotly.subplots import make_subplots
from sidebar import sidebar
import data

st.set_page_config(layout="wide")

#
# Sidebar
#
sidebar()

#
# Main Page
#

# Metrics
metrics = data.getTradingMetrics()
col1, col2, col3, col4 = st.columns(4)

# P&L
pnl = "$" + format(metrics['pnl'], '.2f')
col1.metric("P&L", pnl, pnl)

# Number of trades
col2.metric("Num Trades", metrics['numTrades'], metrics['numTrades'])

# Win rate
winRate = format(metrics['winRate'], '.2f')
col3.metric("Win Rate", winRate, winRate)

# Sharpe
sharpe = format(metrics['sharpe'], '.2f')
col4.metric("Sharpe", sharpe, sharpe)

st.divider()

# Get market data
data_md = data.getMarketData()

# Convert series to pandas DataFrames
timestamps = pd.to_datetime(pd.Series(data_md['series']['Timestamps']['data']), unit='ms')
candles = pd.DataFrame(data_md['series']['Candles']['data'], columns=['open', 'high', 'low', 'close'])
macd = pd.Series(data_md['series']['MACD']['data'])
signal = pd.Series(data_md['series']['Signal']['data'])
position = pd.Series(data_md['series']['Position']['data'])

# Create a subplot for each series
fig = make_subplots(rows=3, cols=1, shared_xaxes=True, subplot_titles=('Market 1m', 'MACD', 'Position'), row_heights=[0.6, 0.2, 0.2])


# Add Candlestick plot
fig.add_trace(
    go.Candlestick(x=timestamps, open=candles['open'], high=candles['high'], low=candles['low'], close=candles['close']),
    row=1, col=1
)

# Add MACD plot
fig.add_trace(
    go.Scatter(x=timestamps, y=macd, mode='lines', line=dict(color=data_md['series']['MACD']['color'])),
    row=2, col=1
)

# Add Signal plot
fig.add_trace(
    go.Scatter(x=timestamps, y=signal, mode='lines', line=dict(color=data_md['series']['Signal']['color'])),
    row=2, col=1
)

# Add Position plot
fig.add_trace(
    go.Scatter(x=timestamps, y=position, mode='lines', line=dict(color=data_md['series']['Position']['color'], shape='hv')),
    row=3, col=1
)

# Update layout
fig.update_layout(height=800, xaxis_rangeslider_visible=False, autosize=False, showlegend=False)

# Show the plot
st.plotly_chart(fig, use_container_width=True)

# Orders Table
header_style = '''
    <style>
    .dataframe th {
        background-color: lightblue;
    }
    </style>
'''
st.write(header_style, unsafe_allow_html=True)
orders_df = data.getOrderData()
st.dataframe(orders_df, use_container_width=True)
