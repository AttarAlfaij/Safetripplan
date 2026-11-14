import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { apiFetch, readJson } from "../api/client.js";
import { LOGOUT_URL, oauthLoginUrl } from "../api/config.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    setLoading(true);
    try {
      const response = await apiFetch("/user");
      const contentType = response.headers.get("content-type");
      if (
        response.ok &&
        contentType &&
        contentType.includes("application/json")
      ) {
        const data = await readJson(response);
        setUser(data || null);
      } else {
        setUser(null);
      }
    } catch {
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const loginWithProvider = useCallback((provider) => {
    window.location.href = oauthLoginUrl(provider);
  }, []);

  const logout = useCallback(async () => {
    try {
      await fetch(LOGOUT_URL, {
        method: "GET",
        credentials: "include",
        cache: "no-store",
        redirect: "manual",
      });
    } finally {
      setUser(null);
      window.location.assign("/");
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      loading,
      refresh,
      loginWithProvider,
      logout,
    }),
    [user, loading, refresh, loginWithProvider, logout]
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
