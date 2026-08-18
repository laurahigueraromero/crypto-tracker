package com.cryptotracker.watchlist;

import jakarta.validation.constraints.NotBlank;

public record AddWatchlistItemRequest(
        @NotBlank(message = "coinId is required")
        String coinId
) {
}
