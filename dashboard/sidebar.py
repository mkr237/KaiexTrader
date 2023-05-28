import streamlit as st
import data

def sidebar():
    with st.sidebar:
        st.markdown("# MACD")
        st.markdown(
            "Fast: 26, Slow: 10, Signal: 9"
        )
        st.markdown("---")

        # Metrics
        metrics = data.getTradingMetrics()
        col1, col2 = st.columns(2)

        # P&L
        pnl = "$" + format(metrics['pnl'], '.2f')
        col1.metric("P&L", pnl, pnl)

        # Number of trades
        col1.metric("Num Trades", metrics['numTrades'], metrics['numTrades'])

        # Win rate
        winRate = format(metrics['winRate'], '.2f')
        col2.metric("Win Rate", winRate, winRate)

        # Sharpe
        sharpe = format(metrics['sharpe'], '.2f')
        col2.metric("Sharpe", sharpe, sharpe)