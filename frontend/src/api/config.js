export const BACKEND_BASE_URL = "http://localhost:8080";

export const API_BASE_URL = "/api";

export const OAUTH_LOGIN_BASE_URL = "/oauth2/authorization";

export const LOGOUT_URL = "/logout";

export const apiUrl = (path = "") => `${API_BASE_URL}${path}`;

export const oauthLoginUrl = (provider) =>
  `${OAUTH_LOGIN_BASE_URL}/${provider}`;

export const backendUrl = (path = "") => `${BACKEND_BASE_URL}${path}`;
