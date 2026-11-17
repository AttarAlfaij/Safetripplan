import { apiUrl } from "./config.js";

export async function apiFetch(path, options = {}) {
  const token = localStorage.getItem("token");
  const headers = {
    Accept: "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...(options.headers || {}),
  };

  const response = await fetch(apiUrl(path), {
    cache: "no-store",
    credentials: "include",
    ...options,
    headers,
  });

  if (response.status === 401 && token) {
    window.dispatchEvent(new CustomEvent("stp:unauthorized"));
  }

  return response;
}

export async function readJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

export async function getErrorMessage(response, fallback) {
  const body = await readJson(response);
  if (body && typeof body.message === "string" && body.message) {
    return body.message;
  }
  if (body && typeof body.error === "string" && body.error) {
    return body.error;
  }
  return fallback;
}
