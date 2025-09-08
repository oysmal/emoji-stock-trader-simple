package no.kraftlauget.kiworkshop

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.system.exitProcess
import io.ktor.client.plugins.*
import io.ktor.http.*

@Volatile
private var shutdownRequested = false

fun main() {
    
    println("=== Phase 1: Team Registration ===")
    
    runBlocking {
        try {
            // Try to load existing credentials first
            val existingCredentials = CredentialStorage.loadCredentials()
            
            val (teamId, apiKey, initialCash) = if (existingCredentials != null) {
                println("Using existing team credentials:")
                println("Team ID: ${existingCredentials.teamId}")
                println("API Key: ${existingCredentials.apiKey}")
                println("Initial Cash: $${existingCredentials.initialCash}")
                
                // Set credentials in ApiClient
                ApiClient.setCredentials(existingCredentials.teamId, existingCredentials.apiKey)
                
                Triple(existingCredentials.teamId, existingCredentials.apiKey, existingCredentials.initialCash)
            } else {
                println("No existing credentials found. Registering new team: ${Config.TEAM_NAME}")
                val response = ApiClient.registerTeam(Config.TEAM_NAME)
                
                // Save credentials for future use
                CredentialStorage.saveCredentials(response.teamId, response.apiKey, response.initialCash)
                
                println("SUCCESS: New team registered!")
                println("Team ID: ${response.teamId}")
                println("API Key: ${response.apiKey}")
                println("Initial Cash: $${response.initialCash}")
                
                Triple(response.teamId, response.apiKey, response.initialCash)
            }
            
            println("\nReady for trading loop implementation...")
            
            println("\n=== Starting Trading Loop ===")
            val tradingBot = TradingBot(ApiClient)

            // Add shutdown hook for graceful exit
            Runtime.getRuntime().addShutdownHook(Thread {
                println("\n=== Shutdown Signal Received ===")
                println("Stopping trading loop gracefully...")
                shutdownRequested = true
                ApiClient.close()
                println("Trading bot shutdown complete.")
            })

            var iterationCount = 0

            while (!shutdownRequested) {
                for (symbol in Config.TRADING_SYMBOLS) {
                    try {
                        val orderBook = ApiClient.getOrderBook(symbol)
                        
                        // First priority: Check for industrial orders (guaranteed profits)
                        val industrialOpportunity = tradingBot.detectIndustrialOrder(orderBook)
                        if (industrialOpportunity != null) {
                            val timestamp = LocalDateTime.now()
                            println("INDUSTRIAL OPPORTUNITY: [$timestamp] ${industrialOpportunity.reason}")
                            
                            when (industrialOpportunity.side) {
                                "BUY" -> {
                                    val buyResult = tradingBot.buyStock(symbol, industrialOpportunity.price, industrialOpportunity.quantity)
                                    if (buyResult != null) {
                                        println("INDUSTRIAL TRADE: [$timestamp] BUY ${industrialOpportunity.quantity} $symbol at $${industrialOpportunity.price} - Order ID: ${buyResult.orderId}")
                                    }
                                }
                                "SELL" -> {
                                    val sellResult = tradingBot.sellStock(symbol, industrialOpportunity.price, industrialOpportunity.quantity)
                                    if (sellResult != null) {
                                        println("INDUSTRIAL TRADE: [$timestamp] SELL ${industrialOpportunity.quantity} $symbol at $${industrialOpportunity.price} - Order ID: ${sellResult.orderId}")
                                    }
                                }
                            }
                        } else {
                            // Normal spread-based trading
                            if (tradingBot.shouldBuyStock(symbol, orderBook)) {
                                val limitPrice = tradingBot.calculateLimitOrderPrice(orderBook, "BUY")
                                if (limitPrice != null) {
                                    val quantity = 10  // Trade 10 shares for meaningful volume
                                    val buyResult = tradingBot.buyStock(symbol, limitPrice, quantity)
                                    if (buyResult != null) {
                                        val timestamp = LocalDateTime.now()
                                        println("TRADE: [$timestamp] BUY $quantity $symbol at $$limitPrice - Order ID: ${buyResult.orderId}")
                                    }
                                }
                            } else if (tradingBot.shouldSellStock(symbol, orderBook)) {
                                val limitPrice = tradingBot.calculateLimitOrderPrice(orderBook, "SELL")
                                if (limitPrice != null) {
                                    val quantity = 10  // Trade 10 shares for meaningful volume
                                    val sellResult = tradingBot.sellStock(symbol, limitPrice, quantity)
                                    if (sellResult != null) {
                                        val timestamp = LocalDateTime.now()
                                        println("TRADE: [$timestamp] SELL $quantity $symbol at $$limitPrice - Order ID: ${sellResult.orderId}")
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        val timestamp = LocalDateTime.now()
                        when (e) {
                            is ClientRequestException -> {
                                println("API ERROR: [$timestamp] GET /v1/orderbook?symbol=$symbol - ${e.response.status.value} ${e.response.status.description}: ${e.message}")
                            }
                            is ServerResponseException -> {
                                println("API ERROR: [$timestamp] GET /v1/orderbook?symbol=$symbol - ${e.response.status.value} ${e.response.status.description}: Server error")
                            }
                            is HttpRequestTimeoutException -> {
                                println("API ERROR: [$timestamp] GET /v1/orderbook?symbol=$symbol - Request timeout after ${Config.REQUEST_TIMEOUT_MS}ms")
                            }
                            else -> {
                                println("API ERROR: [$timestamp] Trading error for $symbol: ${e.message} (${e.javaClass.simpleName})")
                            }
                        }
                    }
                }
                
                iterationCount++
                if (iterationCount % 10 == 0) {
                    val timestamp = LocalDateTime.now()
                    val portfolio = tradingBot.checkPortfolio()
                    if (portfolio != null) {
                        val positionsStr = portfolio.positions.entries.joinToString(", ") { "${it.key}:${it.value}" }
                        println("PORTFOLIO: [$timestamp] Cash: $${String.format("%.2f", portfolio.cash)} | Equity: $${String.format("%.2f", portfolio.equity)} | Positions: $positionsStr")
                    } else {
                        println("PORTFOLIO: [$timestamp] Portfolio unavailable")
                    }
                }
                
                delay(Config.TRADING_INTERVAL_MS)
            }
            
        } catch (e: Exception) {
            println("ERROR: Team registration failed - ${e.message}")
            println("Make sure the emoji stock exchange server is running on ${Config.API_BASE_URL}")
            exitProcess(1)
        }
    }
}