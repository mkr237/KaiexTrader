package kaiex.strategy

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import org.slf4j.LoggerFactory
import java.util.logging.Logger

class CointStrategy(val symbol1: String,
                    val symbol2: String) : Strategy("CointStrategy($symbol1,$symbol2)") {

    private val log: org.slf4j.Logger = LoggerFactory.getLogger(javaClass.simpleName)
    override val config = StrategyChartConfig(
        strategyId = "MACDStrategy/BTC-USD/12/26/9",
        chartConfig = listOf(
            ChartSeriesConfig(
                id = "price",
                seriesType = "candle",
                pane = 0,
                color = "blue"
            ),
            ChartSeriesConfig(
                id = "macd",
                seriesType = "line",
                pane = 1,
                color = "green"
            ),
            ChartSeriesConfig(
                id = "signal",
                seriesType = "line",
                pane = 1,
                color = "red"
            ),
            ChartSeriesConfig(
                id = "histogram",
                seriesType = "histogram",
                pane = 1,
                color = "purple"
            )
        )
    )

    override suspend fun onStart() {
        log.info("onStart()")
    }

    override suspend fun onUpdate() {
        log.info("onUpdate()")
    }

    override suspend fun onStop() {
        log.info("onStop()")
    }
}