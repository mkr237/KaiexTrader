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
#sidebar()

#
# Main Page
#

st.markdown(
    """
    <style>
    /* Reduce spacing at the top */
    .stApp { margin-top: -60px; }
    </style>
    """,
    unsafe_allow_html=True
)

st.header("MACD")
st.write("Fast: 26, Slow: 10, Signal: 9")

st.divider()

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
data_md = data.getChartData()
chartHeight = 800
plotNames = [obj['name'] for obj in data_md['plots']]
plotHeights = [obj['height'] for obj in data_md['plots']]
numPlots = len(data_md['plots'])
timestamps = pd.to_datetime(pd.Series(data_md['timestamps']), unit='ms')

# Create a subplot for each series
fig = make_subplots(rows=numPlots, cols=1, shared_xaxes=True, subplot_titles=plotNames, row_heights=plotHeights)

plotIdx = 1
for plot in data_md['plots']:
    for name, series in plot['seriesData'].items():
        if series['type'] == 'CANDLE':
            df = pd.DataFrame(series['data'], columns=['open', 'high', 'low', 'close'])
            fig.add_trace(
                go.Candlestick(name=name, x=timestamps, open=df['open'], high=df['high'], low=df['low'], close=df['close'], legendgroup=plotIdx),
                row=plotIdx, col=1
            )
        elif series['type'] == 'LINE':
            df = pd.Series(series['data'])
            fig.add_trace(
                go.Scatter(name=name, x=timestamps, y=df, mode='lines', line=dict(color=series['color'], shape=series['shape']), legendgroup=plotIdx),
                row=plotIdx, col=1
            )
        elif series['type'] == 'BAR':
            df = pd.Series(series['data'])
            fig.add_trace(
                go.Bar(name=name, x=timestamps, y=df, marker=dict(color=df.apply(lambda x: '#9FD8CE' if x > 0 else '#FF9C99'),
                                                                  line=dict(color='black', width=1),
                                                                  opacity=df.apply(lambda x: 1.0 if x > 0 else 0.8)), legendgroup=plotIdx),
                row=plotIdx, col=1
            )
        else:
            print("Invalid series type")

    plotIdx = plotIdx + 1

# Update layout
fig.update_layout(height=chartHeight, xaxis_rangeslider_visible=False, autosize=False)

# Show the plot
st.plotly_chart(fig, use_container_width=True)

st.divider()

# Positions Table
header_style = '''
    <style>
    .dataframe th {
        background-color: lightblue;
    }
    </style>
'''
st.write(header_style, unsafe_allow_html=True)
positions_df = data.getPositionsData()
st.subheader(f"Positions ({len(positions_df.data)})")
st.dataframe(positions_df, use_container_width=True)

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
st.subheader(f"Orders  ({len(orders_df.data)})")
st.dataframe(orders_df, use_container_width=True)
