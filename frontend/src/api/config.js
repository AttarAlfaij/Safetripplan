const rawEnvBase = (
  (typeof import.meta !== "undefined" && import.meta.env && import.meta.env.VITE_API_BASE_URL) ||
  (typeof process !== "undefined" && process.env && process.env.VITE_API_BASE_URL) ||
  ""
).trim().replace(/\/+$/, "");

// Extract origin of backend (e.g. https://safetripplan-updatedversion-1.onrender.com or http://localhost:8080)
const backendOrigin = rawEnvBase.endsWith("/api")
  ? rawEnvBase.slice(0, -4)
  : rawEnvBase;

export const BACKEND_BASE_URL = backendOrigin || "http://localhost:8080";

export const API_BASE_URL = rawEnvBase
  ? (rawEnvBase.endsWith("/api") ? rawEnvBase : `${rawEnvBase}/api`)
  : "/api";

export const OAUTH_LOGIN_BASE_URL = backendOrigin
  ? `${backendOrigin}/oauth2/authorization`
  : "/oauth2/authorization";

export const LOGOUT_URL = backendOrigin
  ? `${backendOrigin}/logout`
  : "/logout";

export const AUTH_ENDPOINTS = {
  LOGIN: "/auth/login",
  SIGNUP: "/auth/signup",
  REGISTER: "/auth/register",
  LOGOUT: "/auth/logout",
  ME: "/auth/me",
  USER: "/user",
};

export const apiUrl = (path = "") => {
  const cleanPath = path.startsWith("/") ? path : `/${path}`;
  return `${API_BASE_URL}${cleanPath}`;
};

export const oauthLoginUrl = (provider) =>
  `${OAUTH_LOGIN_BASE_URL}/${provider}`;

export const backendUrl = (path = "") => {
  const cleanPath = path.startsWith("/") ? path : `/${path}`;
  return `${BACKEND_BASE_URL}${cleanPath}`;
};
