package com.cryptotracker.watchlist;

import java.time.Instant;

public record WatchlistItemResponse(
        String coinId,
        Instant addedAt
) {
    public static WatchlistItemResponse from(WatchlistItem item) {
        return new WatchlistItemResponse(item.getCoinId(), item.getCreatedAt());
    }
}
