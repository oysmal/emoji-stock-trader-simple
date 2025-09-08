package no.kraftlauget.kiworkshop

import kotlinx.serialization.Serializable

/**
 * API Registration Response
 */
@Serializable
data class RegisterResponse(
    val teamId: String,
    val apiKey: String,
    val initialCash: Double
)

/**
 * Symbol Information from Exchange
 */
@Serializable
data class SymbolInfo(
    val symbol: String,
    val priceTick: Double,
    val lot: Int,
    val minQuantity: Int,
    val maxQuantity: Int,
    val enabled: Boolean
)

/**
 * Order Book Level (Bid/Ask)
 */
@Serializable
data class OrderBookLevel(
    val price: Double,
    val quantity: Int,
    val orderCount: Int
)

/**
 * Order Book Response
 */
@Serializable
data class OrderBookResponse(
    val symbol: String,
    val bids: List<OrderBookLevel>,
    val asks: List<OrderBookLevel>,
    val timestamp: String
)

/**
 * Place Order Request
 */
@Serializable
data class PlaceOrderRequest(
    val symbol: String,
    val side: String,
    val quantity: Int,
    val limitPrice: Double,
    val timeInForce: String = "GTC"
)

/**
 * Order Response
 */
@Serializable
data class OrderResponse(
    val orderId: String,
    val status: String,
    val symbol: String,
    val side: String,
    val quantity: Int,
    val limitPrice: Double,
    val filledQuantity: Int,
    val avgFillPrice: Double?,
    val remainingQuantity: Int,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Portfolio Response
 */
@Serializable
data class PortfolioResponse(
    val teamId: String,
    val cash: Double,
    val positions: Map<String, Int>,
    val equity: Double,
    val timestamp: String
)

/**
 * Trading Order Side
 */
enum class OrderSide(val value: String) {
    BUY("buy"),
    SELL("sell")
}