import { apiClient } from "./client";

export function register({ email, password, displayName }) {
  return apiClient.post("/auth/register", { email, password, displayName });
}

export function login({ email, password }) {
  return apiClient.post("/auth/login", { email, password });
}

export function logout(refreshToken) {
  return apiClient.post("/auth/logout", { refreshToken });
}
