package com.cryptotracker.watchlist;

import com.cryptotracker.common.ApiException;
import com.cryptotracker.users.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final UserRepository userRepository;

    public WatchlistService(WatchlistRepository watchlistRepository, UserRepository userRepository) {
        this.watchlistRepository = watchlistRepository;
        this.userRepository = userRepository;
    }

    public WatchlistListResponse listForUser(UUID userId) {
        List<WatchlistItemResponse> items = watchlistRepository.findByUser_IdOrderByCreatedAtDesc(userId).stream()
                .map(WatchlistItemResponse::from)
                .toList();
        return new WatchlistListResponse(items);
    }

    public WatchlistAddResult addToWatchlist(UUID userId, String coinId) {
        var existing = watchlistRepository.findByUser_IdAndCoinId(userId, coinId);
        if (existing.isPresent()) {
            return new WatchlistAddResult(WatchlistItemResponse.from(existing.get()), false);
        }

        WatchlistItem item = new WatchlistItem();
        item.setUser(userRepository.getReferenceById(userId));
        item.setCoinId(coinId);

        WatchlistItem saved = watchlistRepository.save(item);
        return new WatchlistAddResult(WatchlistItemResponse.from(saved), true);
    }

    public void removeFromWatchlist(UUID userId, String coinId) {
        WatchlistItem item = watchlistRepository.findByUser_IdAndCoinId(userId, coinId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "WATCHLIST_ITEM_NOT_FOUND", "This coin is not in your watchlist"));
        watchlistRepository.delete(item);
    }
}
