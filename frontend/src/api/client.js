import axios from "axios";
import { clearTokens, getAccessToken } from "./tokenStorage";

const defaultBaseURL = `${window.location.protocol}//${window.location.hostname}:8080/api`;

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || defaultBaseURL,
});

apiClient.interceptors.request.use((config) => {
  const token = getAccessToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      clearTokens();
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  }
);
