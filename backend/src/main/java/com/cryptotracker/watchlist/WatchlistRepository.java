package com.cryptotracker.watchlist;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, UUID> {

    List<WatchlistItem> findByUser_IdOrderByCreatedAtDesc(UUID userId);

    Optional<WatchlistItem> findByUser_IdAndCoinId(UUID userId, String coinId);
}
