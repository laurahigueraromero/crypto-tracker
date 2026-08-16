import { apiClient } from "./client";

export function getCryptoMarkets({ page = 1, perPage = 50, currency = "usd" } = {}) {
  return apiClient.get("/cryptos", { params: { page, perPage, currency } });
}
