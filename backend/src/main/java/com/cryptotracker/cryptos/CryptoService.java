package com.cryptotracker.cryptos;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private final CoinGeckoClient coinGeckoClient;
    private final Cache<String, CryptoListResponse> cache = Caffeine.newBuilder()
            .expireAfterWrite(60, TimeUnit.SECONDS)
            .build();

    public CryptoService(CoinGeckoClient coinGeckoClient) {
        this.coinGeckoClient = coinGeckoClient;
    }

    public CryptoListResponse getMarkets(int page, int perPage, String currency) {
        String key = page + "-" + perPage + "-" + currency;
        return cache.get(key, ignored -> fetchMarkets(page, perPage, currency));
    }

    private CryptoListResponse fetchMarkets(int page, int perPage, String currency) {
        List<CryptoMarketItem> items = coinGeckoClient.fetchMarkets(page, perPage, currency);
        return new CryptoListResponse(page, perPage, items);
    }
}
