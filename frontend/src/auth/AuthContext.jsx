import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { apiFetch, getErrorMessage, readJson } from "../api/client.js";
import {
  AUTH_ENDPOINTS,
  LOGOUT_URL,
  apiUrl,
  oauthLoginUrl,
} from "../api/config.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(() => localStorage.getItem("token"));
  const [loading, setLoading] = useState(() => Boolean(localStorage.getItem("token")));

  const saveAuthSession = useCallback((authToken, authUser) => {
    if (authToken) {
      localStorage.setItem("token", authToken);
      setToken(authToken);
    }
    if (authUser) {
      if (authUser.email) {
        localStorage.setItem("userEmail", authUser.email);
      }
      const resolvedId = authUser.userId || authUser.id;
      if (resolvedId !== undefined && resolvedId !== null) {
        localStorage.setItem("userId", String(resolvedId));
      }
      setUser(authUser);
    }
  }, []);

  const clearAuthSession = useCallback(() => {
    localStorage.removeItem("token");
    localStorage.removeItem("userEmail");
    localStorage.removeItem("userId");
    setToken(null);
    setUser(null);
  }, []);

  const refresh = useCallback(async () => {
    if (localStorage.getItem("token")) {
      setLoading(true);
    }
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 5000);
    try {
      const response = await apiFetch("/user", { signal: controller.signal });
      clearTimeout(timeoutId);
      const contentType = response.headers.get("content-type");
      if (
        response.ok &&
        contentType &&
        contentType.includes("application/json")
      ) {
        const data = await readJson(response);
        if (data && typeof data === "object" && (data.id || data.userId || data.email)) {
          saveAuthSession(data.token, data);
        } else {
          clearAuthSession();
        }
      } else {
        clearAuthSession();
      }
    } catch {
      clearTimeout(timeoutId);
      clearAuthSession();
    } finally {
      setLoading(false);
    }
  }, [clearAuthSession, saveAuthSession]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  useEffect(() => {
    const handleUnauthorized = () => {
      clearAuthSession();
    };

    window.addEventListener("stp:unauthorized", handleUnauthorized);
    return () => {
      window.removeEventListener("stp:unauthorized", handleUnauthorized);
    };
  }, [clearAuthSession]);

  const login = useCallback(
    async (email, password) => {
      const response = await fetch(apiUrl(AUTH_ENDPOINTS.LOGIN), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({ email, password }),
      });

      if (!response.ok) {
        const errorMsg = await getErrorMessage(
          response,
          "Invalid email or password."
        );
        throw new Error(errorMsg);
      }

      const data = await readJson(response);
      if (!data || typeof data !== "object") {
        throw new Error("Invalid response received from authentication server.");
      }

      if (!data.token) {
        throw new Error(data.message || data.error || "Authentication succeeded but no access token was returned.");
      }

      saveAuthSession(data.token, data);
      return data;
    },
    [saveAuthSession]
  );

  const register = useCallback(
    async (email, password, name) => {
      const response = await fetch(apiUrl(AUTH_ENDPOINTS.REGISTER), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Accept: "application/json",
        },
        body: JSON.stringify({ email, password, name }),
      });

      if (!response.ok) {
        const errorMsg = await getErrorMessage(
          response,
          "Registration failed. Please check your inputs."
        );
        throw new Error(errorMsg);
      }

      const data = await readJson(response);
      if (!data || typeof data !== "object") {
        throw new Error("Invalid response received from registration server.");
      }

      if (!data.token) {
        throw new Error(data.message || data.error || "Registration succeeded but no access token was returned.");
      }

      saveAuthSession(data.token, data);
      return data;
    },
    [saveAuthSession]
  );

  const loginWithProvider = useCallback((provider) => {
    window.location.href = oauthLoginUrl(provider);
  }, []);

  const logout = useCallback(async () => {
    try {
      const currentToken = localStorage.getItem("token");
      if (currentToken) {
        await fetch(apiUrl(AUTH_ENDPOINTS.LOGOUT), {
          method: "POST",
          headers: {
            Authorization: `Bearer ${currentToken}`,
            Accept: "application/json",
          },
        }).catch(() => {});
      }
      await fetch(LOGOUT_URL, {
        method: "GET",
        credentials: "include",
        cache: "no-store",
        redirect: "manual",
      }).catch(() => {});
    } finally {
      clearAuthSession();
      window.location.assign("/");
    }
  }, [clearAuthSession]);

  const value = useMemo(
    () => ({
      user,
      token,
      loading,
      login,
      register,
      refresh,
      loginWithProvider,
      logout,
    }),
    [user, token, loading, login, register, refresh, loginWithProvider, logout]
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
