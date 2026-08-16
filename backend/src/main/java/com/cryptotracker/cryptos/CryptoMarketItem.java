package com.cryptotracker.cryptos;

import java.util.List;

public record CryptoMarketItem(
        String coinId,
        String symbol,
        String name,
        String image,
        Double currentPrice,
        Double priceChangePercentage24h,
        List<Double> sparkline7d
) {
}
