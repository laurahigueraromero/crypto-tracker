import { apiClient } from "./client";

export function getProfile() {
  return apiClient.get("/users/me");
}

export function updateProfile({ displayName, avatarUrl, baseCurrency, timezone }) {
  return apiClient.put("/users/me", { displayName, avatarUrl, baseCurrency, timezone });
}
