import { apiClient } from "./client";

export function getWatchlist() {
  return apiClient.get("/watchlist");
}

export function addToWatchlist(coinId) {
  return apiClient.post("/watchlist", { coinId });
}

export function removeFromWatchlist(coinId) {
  return apiClient.delete(`/watchlist/${coinId}`);
}
