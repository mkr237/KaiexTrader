package kaiex.strategy

import kaiex.exchange.simulator.PositionTracker
import kaiex.model.OrderFill
import kaiex.model.OrderRole
import kaiex.model.OrderSide
import kaiex.model.OrderType
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import java.math.BigDecimal

/*
class TradeManagerTest {

    // build up long position and sell back to zero
    @Test
    fun testLong() {

        val tracker = PositionTracker()  // TODO pass in symbol?

        addTrade(tracker, 10f, 100f, OrderSide.BUY)
        assertThat(tracker.positionSize, equals(BigDecimal(100)))
        assertThat(tracker.avgEntryPrice, equals(BigDecimal(10)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        tracker.updatePrice(12f)
        assertThat(tracker.unrealizedPnl, equals(BigDecimal(200)))

        addTrade(tracker, 15f, 50f, OrderSide.BUY)
        assertThat(tracker.positionSize, equals(BigDecimal(150)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.66)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 11f, 20f, OrderSide.BUY)
        assertThat(tracker.positionSize, equals(BigDecimal(170)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.59)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 12f, 30f, OrderSide.SELL)
        assertThat(tracker.positionSize, equals(BigDecimal(140)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.59)))
        assertThat(tracker.avgExitPrice, closeTo(BigDecimal(12)))
        assertThat(tracker.totalExitSize, equals(BigDecimal(30)))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 10f, 40f, OrderSide.SELL)
        assertThat(tracker.positionSize, equals(BigDecimal(100)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.59)))
        assertThat(tracker.avgExitPrice, closeTo(BigDecimal(10.86)))
        assertThat(tracker.totalExitSize, equals(BigDecimal(70)))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 11f, 100f, OrderSide.SELL)
        assertThat(tracker.positionSize, equals(BigDecimal.ZERO))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.59)))
        assertThat(tracker.avgExitPrice, closeTo(BigDecimal(10.94)))
        assertThat(tracker.totalExitSize, equals(BigDecimal(170)))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))
    }

    // build up short position and buy back to zero
    @Test
    fun testShort() {

        val tracker = PositionTracker()  // TODO pass in symbol?

        addTrade(tracker, 10f, 100f, OrderSide.SELL)
        assertThat(tracker.positionSize, equals(BigDecimal(-100)))
        assertThat(tracker.avgEntryPrice, equals(BigDecimal(10)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 15f, 50f, OrderSide.SELL)
        assertThat(tracker.positionSize, equals(BigDecimal(-150)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.66)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 11f, 20f, OrderSide.SELL)
        assertThat(tracker.positionSize, equals(BigDecimal(-170)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.59)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 12f, 30f, OrderSide.BUY)
        assertThat(tracker.positionSize, equals(BigDecimal(-140)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.59)))
        assertThat(tracker.avgExitPrice, closeTo(BigDecimal(12)))
        assertThat(tracker.totalExitSize, equals(BigDecimal(30)))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 10f, 40f, OrderSide.BUY)
        assertThat(tracker.positionSize, equals(BigDecimal(-100)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.59)))
        assertThat(tracker.avgExitPrice, closeTo(BigDecimal(10.86)))
        assertThat(tracker.totalExitSize, equals(BigDecimal(70)))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 11f, 100f, OrderSide.BUY)
        assertThat(tracker.positionSize, equals(BigDecimal.ZERO))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.59)))
        assertThat(tracker.avgExitPrice, closeTo(BigDecimal(10.94)))
        assertThat(tracker.totalExitSize, equals(BigDecimal(170)))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))
    }

    // open long position and sell to below zero
    @Test
    fun testLongToShort() {

        val tracker = PositionTracker()  // TODO pass in symbol?

        addTrade(tracker, 10f, 100f, OrderSide.BUY)
        assertThat(tracker.positionSize, equals(BigDecimal(100)))
        assertThat(tracker.avgEntryPrice, equals(BigDecimal(10)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 15f, 50f, OrderSide.BUY)
        assertThat(tracker.positionSize, equals(BigDecimal(150)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.66)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 12f, 170f, OrderSide.SELL)
        assertThat(tracker.positionSize, equals(BigDecimal(-20)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(12)))
        assertThat(tracker.avgExitPrice, closeTo(BigDecimal(12)))
        assertThat(tracker.totalExitSize, equals(BigDecimal(150)))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))
    }

    // open short position and buy to above zero
    @Test
    fun testShortToLong() {

        val tracker = PositionTracker()  // TODO pass in symbol?

        addTrade(tracker, 10f, 100f, OrderSide.SELL)
        assertThat(tracker.positionSize, equals(BigDecimal(-100)))
        assertThat(tracker.avgEntryPrice, equals(BigDecimal(10)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 15f, 50f, OrderSide.SELL)
        assertThat(tracker.positionSize, equals(BigDecimal(-150)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(11.66)))
        assertThat(tracker.avgExitPrice, equals(BigDecimal.ZERO))
        assertThat(tracker.totalExitSize, equals(BigDecimal.ZERO))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))

        addTrade(tracker, 12f, 170f, OrderSide.BUY)
        assertThat(tracker.positionSize, equals(BigDecimal(20)))
        assertThat(tracker.avgEntryPrice, closeTo(BigDecimal(12)))
        assertThat(tracker.avgExitPrice, closeTo(BigDecimal(12)))
        assertThat(tracker.totalExitSize, equals(BigDecimal(150)))
        assertThat(tracker.realizedPnl, equals(BigDecimal.ZERO))
        assertThat(tracker.unrealizedPnl, equals(BigDecimal.ZERO))
    }

    private fun addTrade(tracker: PositionTracker, price: Float, size: Float, side: OrderSide) {
       tracker.addTrade(OrderFill("1", "1", "AAPL", OrderType.MARKET, side, price, size, 0f, OrderRole.TAKER, 0L, 0L))
    }

    private fun equals(value: BigDecimal): Matcher<BigDecimal> = Matchers.comparesEqualTo(value)
    private fun closeTo(value: BigDecimal): Matcher<BigDecimal> = Matchers.closeTo(value, BigDecimal(0.01))
}
*/