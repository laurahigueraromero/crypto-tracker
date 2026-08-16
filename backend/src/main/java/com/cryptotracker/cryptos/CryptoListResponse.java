package com.cryptotracker.cryptos;

import java.util.List;

public record CryptoListResponse(
        int page,
        int perPage,
        List<CryptoMarketItem> items
) {
}
