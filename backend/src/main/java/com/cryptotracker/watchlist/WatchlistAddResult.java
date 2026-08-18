package com.cryptotracker.watchlist;

public record WatchlistAddResult(
        WatchlistItemResponse item,
        boolean created
) {
}
