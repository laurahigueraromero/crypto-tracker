import { apiClient } from "./client";

export function register({ email, password, displayName }) {
  return apiClient.post("/auth/register", { email, password, displayName });
}
