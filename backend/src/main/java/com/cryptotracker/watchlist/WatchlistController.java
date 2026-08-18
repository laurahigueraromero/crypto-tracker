package com.cryptotracker.watchlist;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping
    public WatchlistListResponse list(@AuthenticationPrincipal UUID userId) {
        return watchlistService.listForUser(userId);
    }

    @PostMapping
    public ResponseEntity<WatchlistItemResponse> add(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody AddWatchlistItemRequest request
    ) {
        WatchlistAddResult result = watchlistService.addToWatchlist(userId, request.coinId());
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.item());
    }

    @DeleteMapping("/{coinId}")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String coinId
    ) {
        watchlistService.removeFromWatchlist(userId, coinId);
        return ResponseEntity.noContent().build();
    }
}
