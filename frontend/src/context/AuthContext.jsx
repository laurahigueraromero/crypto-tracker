import { createContext, useContext, useMemo, useState } from "react";
import { clearTokens, getAccessToken, setTokens } from "../api/tokenStorage";

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
      logout() {
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
