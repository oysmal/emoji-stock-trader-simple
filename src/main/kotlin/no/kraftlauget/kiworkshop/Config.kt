package no.kraftlauget.kiworkshop

/**
 * Trading Bot Configuration
 * Simple object with sensible defaults for 3-hour workshop
 */
object Config {
    
    /**
     * API Configuration
     */
    val API_BASE_URL: String = System.getenv("API_BASE_URL") ?: "http://localhost:8080"
    
    /**
     * Trading Configuration
     */
    val ORDER_SIZE_USD: Double = System.getenv("ORDER_SIZE_USD")?.toDoubleOrNull() ?: 100.0
    val TRADING_INTERVAL_MS: Long = System.getenv("TRADING_INTERVAL_MS")?.toLongOrNull() ?: 5000L
    
    /**
     * Team Configuration
     */
    val TEAM_NAME: String = System.getenv("TEAM_NAME") ?: "awesome-team"
    
    /**
     * Trading Symbols (Emojis)
     */
    val TRADING_SYMBOLS: List<String> = listOf("🦄", "💎", "❤️", "🍌", "🍾", "💻")
    
    /**
     * API Endpoints
     */
    val REGISTER_ENDPOINT: String = "$API_BASE_URL/register"
    val SYMBOLS_ENDPOINT: String = "$API_BASE_URL/symbols"
    val ORDERBOOK_ENDPOINT: String = "$API_BASE_URL/orderbook"
    val ORDERS_ENDPOINT: String = "$API_BASE_URL/orders"
    val PORTFOLIO_ENDPOINT: String = "$API_BASE_URL/portfolio"
    
    /**
     * HTTP Configuration
     */
    val REQUEST_TIMEOUT_MS: Long = 10000L
    val MAX_RETRIES: Int = 3
    
    /**
     * Logging Configuration
     */
    val LOG_LEVEL: String = System.getenv("LOG_LEVEL") ?: "INFO"
    
    /**
     * Print current configuration for debugging
     */
    fun printConfig() {
        println("=== Trading Bot Configuration ===")
        println("API Base URL: $API_BASE_URL")
        println("Team Name: $TEAM_NAME")
        println("Order Size: $ORDER_SIZE_USD USD")
        println("Trading Interval: ${TRADING_INTERVAL_MS}ms")
        println("Trading Symbols: ${TRADING_SYMBOLS.joinToString(", ")}")
        println("Request Timeout: ${REQUEST_TIMEOUT_MS}ms")
        println("Max Retries: $MAX_RETRIES")
        println("==================================")
    }
}