package com.cryptotracker.cryptos;

import com.cryptotracker.common.ApiException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;

@Component
public class CoinGeckoClient {

    private final RestClient restClient;

    public CoinGeckoClient(RestClient coinGeckoRestClient) {
        this.restClient = coinGeckoRestClient;
    }

    public List<CryptoMarketItem> fetchMarkets(int page, int perPage, String currency) {
        JsonNode body;
        try {
            body = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/coins/markets")
                            .queryParam("vs_currency", currency)
                            .queryParam("order", "market_cap_desc")
                            .queryParam("page", page)
                            .queryParam("per_page", perPage)
                            .queryParam("sparkline", true)
                            .queryParam("price_change_percentage", "24h")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "MARKET_DATA_UNAVAILABLE",
                    "Market data is not available at this time");
        }

        if (body == null || !body.isArray()) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "MARKET_DATA_UNAVAILABLE",
                    "Market data is not available at this time");
        }

        List<CryptoMarketItem> items = new ArrayList<>();
        for (JsonNode node : body) {
            items.add(toMarketItem(node));
        }
        return items;
    }

    private CryptoMarketItem toMarketItem(JsonNode node) {
        List<Double> sparkline = new ArrayList<>();
        JsonNode sparklineNode = node.path("sparkline_in_7d").path("price");
        if (sparklineNode.isArray()) {
            for (JsonNode price : sparklineNode) {
                sparkline.add(price.asDouble());
            }
        }

        return new CryptoMarketItem(
                node.path("id").asString(""),
                node.path("symbol").asString(""),
                node.path("name").asString(""),
                node.path("image").asString(""),
                node.path("current_price").isMissingNode() ? null : node.path("current_price").asDouble(),
                node.path("price_change_percentage_24h").isMissingNode() ? null : node.path("price_change_percentage_24h").asDouble(),
                sparkline
        );
    }
}
