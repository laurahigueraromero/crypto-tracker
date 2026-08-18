import { createContext, useContext, useMemo, useState } from "react";
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from "../api/tokenStorage";
import { logout as logoutRequest } from "../api/auth";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(getAccessToken()));

  const value = useMemo(
    () => ({
      isAuthenticated,
      login(tokens) {
        setTokens(tokens);
        setIsAuthenticated(true);
      },
      async logout() {
        const refreshToken = getRefreshToken();
        if (refreshToken) {
          try {
            await logoutRequest(refreshToken);
          } catch {
            // Best-effort: even if the server call fails (offline, expired
            // token, etc.), the user's local session still gets cleared.
          }
        }
        clearTokens();
        setIsAuthenticated(false);
      },
    }),
    [isAuthenticated]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
