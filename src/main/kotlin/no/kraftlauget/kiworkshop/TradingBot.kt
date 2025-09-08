package no.kraftlauget.kiworkshop

/**
 * Trading Bot for Emoji Stock Exchange
 * Orchestrates automated trading operations using the API client
 */
class TradingBot(private val apiClient: ApiClient) {
    
    /**
     * Calculate average price from order book using midpoint of best bid and ask.
     * Uses defensive approach that keeps money moving when only one side exists.
     * 
     * @param orderBook The order book data
     * @return Average price, or 0.0 if both bids and asks are empty
     */
    fun calculateAveragePrice(orderBook: OrderBookResponse): Double {
        val bestBid = orderBook.bids.maxByOrNull { it.price }?.price
        val bestAsk = orderBook.asks.minByOrNull { it.price }?.price
        
        return when {
            bestBid != null && bestAsk != null -> (bestBid + bestAsk) / 2.0
            bestBid != null -> bestBid  // Only bids exist, use best bid
            bestAsk != null -> bestAsk  // Only asks exist, use best ask
            else -> 0.0  // Both empty
        }
    }
    
    /**
     * Determines if we should buy a stock based on spread analysis.
     * Buys when there's sufficient liquidity and profitable spread opportunities.
     * 
     * @param symbol The stock symbol to evaluate
     * @param orderBook The current order book data
     * @return true if we should buy, false otherwise
     */
    fun shouldBuyStock(symbol: String, orderBook: OrderBookResponse): Boolean {
        // Need both bids and asks to analyze spread
        if (orderBook.bids.isEmpty() || orderBook.asks.isEmpty()) {
            return false
        }
        
        val bestBid = orderBook.bids.maxByOrNull { it.price }?.price ?: return false
        val bestAsk = orderBook.asks.minByOrNull { it.price }?.price ?: return false
        
        // Calculate spread
        val spread = bestAsk - bestBid
        
        // Only buy if there's a meaningful spread (> $0.10) indicating opportunity
        if (spread <= 0.10) {
            return false
        }
        
        // Check if there's good liquidity on the ask side (at least 10 shares available)
        val askQuantity = orderBook.asks.minByOrNull { it.price }?.quantity ?: 0
        if (askQuantity < 10) {
            return false
        }
        
        // Buy decision: spread is wide enough and there's liquidity
        return true
    }
    
    /**
     * Determines if we should sell a stock based on spread analysis.
     * Sells when there's sufficient liquidity and profitable spread opportunities.
     * 
     * @param symbol The stock symbol to evaluate
     * @param orderBook The current order book data
     * @return true if we should sell, false otherwise
     */
    fun shouldSellStock(symbol: String, orderBook: OrderBookResponse): Boolean {
        // Need both bids and asks to analyze spread
        if (orderBook.bids.isEmpty() || orderBook.asks.isEmpty()) {
            return false
        }
        
        val bestBid = orderBook.bids.maxByOrNull { it.price }?.price ?: return false
        val bestAsk = orderBook.asks.minByOrNull { it.price }?.price ?: return false
        
        // Calculate spread
        val spread = bestAsk - bestBid
        
        // Only sell if there's a meaningful spread (> $0.10) indicating opportunity
        if (spread <= 0.10) {
            return false
        }
        
        // Check if there's good liquidity on the bid side (at least 10 shares demanded)
        val bidQuantity = orderBook.bids.maxByOrNull { it.price }?.quantity ?: 0
        if (bidQuantity < 10) {
            return false
        }
        
        // Sell decision: spread is wide enough and there's liquidity
        return true
    }
    
    /**
     * Detects industrial orders (large orders > 100 shares) in the order book.
     * These create immediate arbitrage opportunities due to temporary price imbalances.
     * 
     * @param orderBook The current order book data
     * @return IndustrialOrderOpportunity if found, null otherwise
     */
    fun detectIndustrialOrder(orderBook: OrderBookResponse): IndustrialOrderOpportunity? {
        // Check for large ask orders (big sell orders = cheap buy opportunity)
        val largeAsk = orderBook.asks.find { it.quantity > 100 || it.orderCount > 1 }
        if (largeAsk != null) {
            val roundedPrice = kotlin.math.round(largeAsk.price * 100.0) / 100.0
            println("Industrial BUY opportunity detected: ${largeAsk.quantity} shares at ${"%.2f".format(roundedPrice)}")
            return IndustrialOrderOpportunity(
                side = "BUY",
                price = roundedPrice,
                quantity = minOf(largeAsk.quantity, 50), // Cap our order size
                reason = "Large ask order (${largeAsk.quantity} shares)"
            )
        }
        
        // Check for large bid orders (big buy orders = good sell opportunity)
        val largeBid = orderBook.bids.find { it.quantity > 100 || it.orderCount > 1 }
        if (largeBid != null) {
            val roundedPrice = kotlin.math.round(largeBid.price * 100.0) / 100.0
            println("Industrial SELL opportunity detected: ${largeBid.quantity} shares at ${"%.2f".format(roundedPrice)}")
            return IndustrialOrderOpportunity(
                side = "SELL",
                price = roundedPrice,
                quantity = minOf(largeBid.quantity, 50), // Cap our order size
                reason = "Large bid order (${largeBid.quantity} shares)"
            )
        }
        
        return null
    }
    
    /**
     * Data class representing an industrial order opportunity
     */
    data class IndustrialOrderOpportunity(
        val side: String,
        val price: Double,
        val quantity: Int,
        val reason: String
    )
    
    /**
     * Calculate optimal limit order price to place in the spread.
     * For buy orders: place bid slightly above current best bid
     * For sell orders: place ask slightly below current best ask
     * This captures spread profits instead of paying market prices.
     * 
     * @param orderBook The current order book data
     * @param side "BUY" or "SELL"
     * @return Optimal limit price, or null if no valid spread exists
     */
    fun calculateLimitOrderPrice(orderBook: OrderBookResponse, side: String): Double? {
        if (orderBook.bids.isEmpty() || orderBook.asks.isEmpty()) {
            return null
        }
        
        val bestBid = orderBook.bids.maxByOrNull { it.price }?.price ?: return null
        val bestAsk = orderBook.asks.minByOrNull { it.price }?.price ?: return null
        
        val spread = bestAsk - bestBid
        
        // Only trade if spread is meaningful (> $0.10)
        if (spread <= 0.10) {
            return null
        }
        
        return when (side) {
            "BUY" -> {
                // Place buy order in middle of spread, slightly favoring bid side
                val targetPrice = bestBid + (spread * 0.3) // 30% into the spread
                val roundedPrice = (kotlin.math.round(targetPrice * 100.0) / 100.0).let { 
                    // Ensure proper 2-decimal formatting by rounding to nearest cent
                    kotlin.math.round(it * 100.0) / 100.0 
                }
                println("Calculated BUY limit price: ${"%.2f".format(roundedPrice)} (spread: $bestBid - $bestAsk)")
                roundedPrice
            }
            "SELL" -> {
                // Place sell order in middle of spread, slightly favoring ask side  
                val targetPrice = bestAsk - (spread * 0.3) // 30% down from ask
                val roundedPrice = (kotlin.math.round(targetPrice * 100.0) / 100.0).let { 
                    // Ensure proper 2-decimal formatting by rounding to nearest cent
                    kotlin.math.round(it * 100.0) / 100.0 
                }
                println("Calculated SELL limit price: ${"%.2f".format(roundedPrice)} (spread: $bestBid - $bestAsk)")
                roundedPrice
            }
            else -> null
        }
    }
    
    /**
     * Execute a buy order for a stock.
     * 
     * @param symbol The stock symbol to buy
     * @param price The limit price for the order
     * @param quantity The number of shares to buy
     * @return OrderResponse on success, null on failure
     */
    suspend fun buyStock(symbol: String, price: Double, quantity: Int): OrderResponse? {
        // Validate order parameters
        if (quantity <= 0) {
            println("Buy order rejected: Invalid quantity $quantity for $symbol")
            return null
        }
        
        if (price <= 0.0) {
            println("Buy order rejected: Invalid price $price for $symbol")
            return null
        }
        
        // Check if we have enough cash
        val portfolio = checkPortfolio() ?: run {
            println("Buy order rejected: Could not retrieve portfolio for $symbol")
            return null
        }
        
        val totalCost = price * quantity
        if (portfolio.cash < totalCost) {
            println("Buy order rejected: Insufficient cash. Need $totalCost, have ${portfolio.cash} for $quantity $symbol")
            return null
        }
        
        return try {
            val request = PlaceOrderRequest(
                symbol = symbol,
                side = "BUY",
                quantity = quantity,
                limitPrice = price
            )
            
            val response = apiClient.placeOrder(request)
            println("Buy order successful: ${response.orderId} for $quantity $symbol at $price")
            response
            
        } catch (e: Exception) {
            println("Buy order failed for $quantity $symbol at $price: ${e.message}")
            null
        }
    }
    
    /**
     * Execute a sell order for a stock.
     * 
     * @param symbol The stock symbol to sell
     * @param price The limit price for the order
     * @param quantity The number of shares to sell
     * @return OrderResponse on success, null on failure
     */
    suspend fun sellStock(symbol: String, price: Double, quantity: Int): OrderResponse? {
        // Validate order parameters
        if (quantity <= 0) {
            println("Sell order rejected: Invalid quantity $quantity for $symbol")
            return null
        }
        
        if (price <= 0.0) {
            println("Sell order rejected: Invalid price $price for $symbol")
            return null
        }
        
        // Check if we have enough shares to sell
        val portfolio = checkPortfolio() ?: run {
            println("Sell order rejected: Could not retrieve portfolio for $symbol")
            return null
        }
        
        val currentPosition = portfolio.positions[symbol] ?: 0
        if (currentPosition < quantity) {
            println("Sell order rejected: Insufficient shares. Need $quantity, have $currentPosition for $symbol")
            return null
        }
        
        return try {
            val request = PlaceOrderRequest(
                symbol = symbol,
                side = "SELL",
                quantity = quantity,
                limitPrice = price
            )
            
            val response = apiClient.placeOrder(request)
            println("Sell order successful: ${response.orderId} for $quantity $symbol at $price")
            response
            
        } catch (e: Exception) {
            println("Sell order failed for $quantity $symbol at $price: ${e.message}")
            null
        }
    }
    
    /**
     * Check current portfolio positions and balances.
     * 
     * @return PortfolioResponse on success, null on failure
     */
    suspend fun checkPortfolio(): PortfolioResponse? {
        return try {
            val portfolio = apiClient.getPortfolio()
            println("Portfolio check successful - Cash: ${portfolio.cash}, Equity: ${portfolio.equity}, Positions: ${portfolio.positions}")
            portfolio
            
        } catch (e: Exception) {
            println("Portfolio check failed: ${e.message}")
            null
        }
    }
    
    /**
     * Calculate the number of shares to buy based on available cash and stock price.
     * Uses simple division with basic validation for position sizing.
     * 
     * @param availableCash The available cash for trading
     * @param price The current stock price
     * @return Number of shares that can be purchased, or 0 if price is invalid
     */
    fun calculateOrderSize(availableCash: Double, price: Double): Int {
        if (price <= 0.0) return 0
        val amountToSpend = minOf(availableCash, Config.ORDER_SIZE_USD)
        return (amountToSpend / price).toInt()
    }
    
}