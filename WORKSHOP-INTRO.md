# 🚀 Emoji Stock Exchange Workshop - Getting Started

## What You're Building

You'll create a trading bot that autonomously buys and sells emoji stocks (🦄, 💎, ❤️, 🍌, 🍾, 💻) to maximize profit. Your bot will compete against others in real-time on a live exchange.

## How the Exchange Works

- **Starting Position**: $10,000 cash + 100 shares of each emoji symbol
- **Goal**: Maximize your total equity (cash + stock value)
- **Trading**: Submit buy/sell limit orders via REST API
- **Competition**: Real-time leaderboard shows P&L rankings
- **Rate Limit**: 50 requests/second per team

### Order Book Mechanics

The exchange operates as a **central limit order book** with price-time priority:

- **Bids (Buy Orders)**: Sorted highest price first, then by time
- **Asks (Sell Orders)**: Sorted lowest price first, then by time  
- **Order Matching**: Incoming orders match against best opposite price
- **Spread**: Gap between best bid and best ask (e.g., bid $10.00, ask $10.50)

**Key Insight**: You buy at the **ask price**, sell at the **bid price**

### Order Book Example
```
Symbol: 🦄
Asks (Sellers):  $10.75 (50 shares)
                 $10.50 (100 shares)  ← Best Ask
                 ─────────────────────
Bids (Buyers):   $10.00 (75 shares)   ← Best Bid
                 $9.75  (200 shares)
```

- To **buy immediately**: Submit market buy → executes at $10.50
- To **sell immediately**: Submit market sell → executes at $10.00
- **Spread cost**: $0.50 per share ($10.50 - $10.00)

## Resources

- **API Documentation**: [Swagger UI](http://localhost:8080/docs) - Interactive API explorer
- **Exchange URL**: `http://localhost:8080`
- **Leaderboard**: Check `/leaderboard` endpoint for rankings

## Workshop Tasks

### 1. 🔌 **Registration & Connection** (30 min)
- Register your team with a unique team ID
- Get your API key for authentication
- Test basic connectivity

### 2. 📊 **Submit Your First Orders** (30 min)
- Learn order submission format
- Place buy and sell limit orders
- View orders in the order book

### 3. 💼 **Track Your Portfolio** (30 min)
- Monitor cash and stock positions
- Track trade executions (fills)
- Calculate profit & loss

### 4. 🎯 **Build a Simple Strategy** (30 min)
- Understand bid-ask spread mechanics
- Implement profitable trading logic
- Add position and cash management

**Strategy Fundamentals**:
- **Profitable Buy**: Place bid below current best ask
- **Profitable Sell**: Place ask above current best bid
- **Avoid Market Orders**: They execute immediately at worst price
- **Example**: If spread is $10.00-$10.50, place bid at $10.25, ask at $10.25

### 5. 🏭 **Detect Industrial Orders** (30 min)
- Spot large orders (quantity > 100) in the order book
- React quickly to guaranteed profit opportunities
- Compete for the best execution

**Industrial Order Detection**:
```bash
# Watch for large orders in order book
GET /v1/orderbook?symbol=🦄

# Look for orderCount > 1 OR quantity > 100
{
  "asks": [
    {"price": 10.50, "quantity": 250, "orderCount": 1}  ← Industrial!
  ]
}
```

**Why Industrial Orders = Free Money**:
- Large order creates temporary price imbalance
- You can buy/sell against it at favorable price
- Other bots compete, so **speed matters**

## Success Tips

### Trading Strategy
- **Understand the spread** - Always know bid vs ask prices
- **Never market buy/sell** - Use limit orders to control price
- **Target the middle** - Place orders between bid and ask
- **Watch position limits** - Don't overexpose to one symbol

### Industrial Order Strategy  
- **Monitor order book depth** - Look for quantity > 100
- **React fast** - Other teams compete for same opportunities
- **Calculate profit first** - Ensure spread > transaction costs

### Development Tips
- **Start with manual testing** - Use Swagger UI first
- **Test your math** - Verify profit calculations
- **Check the leaderboard** - Track performance vs others
- **Handle rate limits** - 50 req/sec max

### Common Mistakes to Avoid
- ❌ Buying at ask, selling at bid (guarantees loss)
- ❌ Using market orders (worst execution price)
- ❌ Ignoring position limits (overconcentration risk)
- ❌ Not validating profit before placing orders

## Available Symbols
- 🦄 Unicorn
- 💎 Diamond  
- ❤️ Heart
- 🍌 Banana
- 🍾 Champagne
- 💻 Computer

## API Quick Reference

### Place Limit Order
```bash
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -H "X-Team-Id: YOUR_TEAM_ID" \
  -H "X-Api-Key: YOUR_API_KEY" \
  -d '{
    "side": "BUY",
    "symbol": "🦄", 
    "quantity": 50,
    "limitPrice": 10.25,
    "timeInForce": "GTC"
  }'
```

### Check Order Book
```bash
# See all current orders (requires authentication)
curl "http://localhost:8080/v1/orderbook?symbol=🦄&depth=10" \
  -H "X-Team-Id: YOUR_TEAM_ID" \
  -H "X-Api-Key: YOUR_API_KEY"

# Response shows bids (buyers) and asks (sellers)
{
  "bids": [{"price": 10.00, "quantity": 75, "orderCount": 1}],
  "asks": [{"price": 10.50, "quantity": 100, "orderCount": 1}]
}
```

### Monitor Your Portfolio
```bash
curl http://localhost:8080/v1/portfolio/YOUR_TEAM_ID \
  -H "X-Team-Id: YOUR_TEAM_ID" \
  -H "X-Api-Key: YOUR_API_KEY"
```

### Watch for Fills (Executions)
```bash
curl http://localhost:8080/v1/fills \
  -H "X-Team-Id: YOUR_TEAM_ID" \
  -H "X-Api-Key: YOUR_API_KEY"
```

**Ready to trade? Let's build your bot! 📈**