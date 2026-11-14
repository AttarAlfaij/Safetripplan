import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { BACKEND_BASE_URL } from "./src/api/config.js";

const backendTarget = {
  target: BACKEND_BASE_URL,
  changeOrigin: true,
};

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": backendTarget,
      "/oauth2": backendTarget,
      "/logout": backendTarget,
    },
  },
  preview: {
    port: 4173,
    proxy: {
      "/api": backendTarget,
      "/oauth2": backendTarget,
      "/logout": backendTarget,
    },
  },
});
