package no.kraftlauget.kiworkshop

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * API Client for Emoji Stock Exchange
 * Handles all HTTP communication with the trading platform
 */
object ApiClient {
    
    // Authentication storage - set after team registration
    var teamId: String? = null
        private set
    var apiKey: String? = null  
        private set
    
    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = Config.REQUEST_TIMEOUT_MS
        }
    }
    
    /**
     * Set credentials manually (for loading from storage)
     */
    fun setCredentials(teamId: String, apiKey: String) {
        this.teamId = teamId
        this.apiKey = apiKey
        println("API client configured with stored credentials")
    }
    
    /**
     * Register team with the exchange and store authentication credentials
     */
    suspend fun registerTeam(teamName: String): RegisterResponse {
        println("Registering team: $teamName")
        
        try {
            val response: RegisterResponse = httpClient.post("${Config.API_BASE_URL}/v1/register") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("teamId" to teamName))
            }.body()
            
            // Store authentication credentials
            teamId = response.teamId
            apiKey = response.apiKey
            
            println("Team registered successfully - TeamID: ${response.teamId}, Cash: ${response.initialCash}")
            return response
            
        } catch (e: Exception) {
            println("Failed to register team: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get available trading symbols from the exchange
     */
    suspend fun getSymbols(): List<SymbolInfo> {
        println("Fetching available symbols")
        
        try {
            val response: List<SymbolInfo> = httpClient.get("${Config.API_BASE_URL}/v1/symbols").body()
            println("Retrieved ${response.size} symbols")
            return response
            
        } catch (e: Exception) {
            println("Failed to get symbols: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get order book for a specific symbol
     */
    suspend fun getOrderBook(symbol: String): OrderBookResponse {
        println("Fetching order book for symbol: $symbol")
        
        try {
            val response: OrderBookResponse = httpClient.get("${Config.API_BASE_URL}/v1/orderbook") {
                parameter("symbol", symbol)
                addAuthHeaders()
            }.body()
            
            println("Order book for $symbol - Bids: ${response.bids.size}, Asks: ${response.asks.size}")
            return response
            
        } catch (e: Exception) {
            println("Failed to get order book for $symbol: ${e.message}")
            throw e
        }
    }
    
    /**
     * Place a trading order
     */
    suspend fun placeOrder(request: PlaceOrderRequest): OrderResponse {
        println("Placing ${request.side} order for ${request.quantity} ${request.symbol} at ${request.limitPrice}")
        
        try {
            val httpResponse = httpClient.post("${Config.API_BASE_URL}/v1/orders") {
                contentType(ContentType.Application.Json)
                setBody(request)
                addAuthHeaders()
            }
            
            if (httpResponse.status.isSuccess()) {
                val response: OrderResponse = httpResponse.body()
                println("Order placed successfully - OrderID: ${response.orderId}, Status: ${response.status}")
                return response
            } else {
                val errorText = httpResponse.body<String>()
                println("Order rejected by API - Status: ${httpResponse.status}, Response: $errorText")
                throw Exception("Order rejected: ${httpResponse.status} - $errorText")
            }
            
        } catch (e: Exception) {
            println("Failed to place order: ${e.message}")
            throw e
        }
    }
    
    /**
     * Get current portfolio using stored team ID
     */
    suspend fun getPortfolio(): PortfolioResponse {
        val currentTeamId = teamId ?: throw IllegalStateException("Team not registered - call registerTeam() first")
        println("Fetching portfolio for team: $currentTeamId")
        
        try {
            val response: PortfolioResponse = httpClient.get("${Config.API_BASE_URL}/v1/portfolio/$currentTeamId") {
                addAuthHeaders()
            }.body()
            
            println("Portfolio retrieved - Cash: ${response.cash}, Equity: ${response.equity}, Positions: ${response.positions.size}")
            return response
            
        } catch (e: Exception) {
            println("Failed to get portfolio: ${e.message}")
            throw e
        }
    }
    
    /**
     * Add authentication headers to request
     */
    private fun HttpRequestBuilder.addAuthHeaders() {
        val currentTeamId = teamId ?: throw IllegalStateException("Team not registered")
        val currentApiKey = apiKey ?: throw IllegalStateException("API key not available")
        
        headers {
            append("X-Team-Id", currentTeamId)
            append("X-Api-Key", currentApiKey)
        }
    }
    
    /**
     * Check if client is authenticated
     */
    fun isAuthenticated(): Boolean = teamId != null && apiKey != null
    
    /**
     * Close the HTTP client when done
     */
    fun close() {
        httpClient.close()
    }
}