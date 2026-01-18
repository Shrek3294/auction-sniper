# Market Research: Donut Auction

Based on real-time analysis of `https://donut.auction`, here are the best items to target for sniping.
*Note: The "Value" listed on the homepage is often inaccurate. These targets are based on finding prices significantly below the recent transaction average.*

## 🎯 Top Sniping Targets

| Item | Average Trade Price | Sniping Target Price | Potential Profit (per item) | Volume |
| :--- | :--- | :--- | :--- | :--- |
| **Elytra** | $344,000,000 | **<$290,000,000** | ~$50,000,000+ | Low |
| **Dragon Head** | $55,000,000 | **<$45,000,000** | ~$10,000,000 | Medium |
| **Netherite Ingot** | $7,300,000 | **<$6,000,000** | ~$1,300,000 | **High** |
| **Ancient Debris** | $2,000,000 | **<$1,600,000** | ~$400,000 | **High** |
| **Shulker Box** | $100,000 | **<$75,000** | ~$25,000 | Medium |

## 💡 Strategy Recommendations

### 1. The "Safe" Strategy (Netherite/Debris)
-   **Target**: Netherite Scrap, Ancient Debris, Netherite Ingots.
-   **Why**: These sell *constantly*. If you buy one for 10% off, you can resell it almost instantly.
-   **Risk**: Low. Prices are stable.

### 2. The "Deeps" Strategy (Elytras)
-   **Target**: Elytra.
-   **Why**: People sometimes list these for $100M instead of $300M by mistake (missing a zero).
-   **Reward**: One snipe can make you rich.
-   **Risk**: You need a lot of money to buy even one.

## ⚙️ Recommended Config
For your first test, I recommend targeting **Ancient Debris** or **Netherite Ingot** because they appear frequently, so we can verify the scanner is working quickly.

```json
{
  "targetItemName": "Ancient Debris",
  "maxPrice": 1600000.0,
  "refreshIntervalMin": 1000,
  "refreshIntervalMax": 1500
}
```
