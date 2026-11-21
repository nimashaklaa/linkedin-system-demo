import { defineConfig } from "vite";

export default defineConfig({
  server: {
    proxy: {
      "/api/v1/auth": "http://localhost:8081",
      "/api/v1/notifications": "http://localhost:8085"
    }
  },
  build: {
    target: "esnext"
  }
});
