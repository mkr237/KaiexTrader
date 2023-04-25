package kaiex.exchange.simulator.adapters

import kaiex.model.Trade

interface TickAdapter {
    fun convert(line: String): Trade
}