package com.cryptotracker.watchlist;

import java.util.List;

public record WatchlistListResponse(
        List<WatchlistItemResponse> items
) {
}
