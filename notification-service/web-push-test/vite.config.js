import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { defineConfig, loadEnv } from "vite";

const firebaseEnvKeys = {
  apiKey: "VITE_FIREBASE_API_KEY",
  authDomain: "VITE_FIREBASE_AUTH_DOMAIN",
  projectId: "VITE_FIREBASE_PROJECT_ID",
  storageBucket: "VITE_FIREBASE_STORAGE_BUCKET",
  messagingSenderId: "VITE_FIREBASE_MESSAGING_SENDER_ID",
  appId: "VITE_FIREBASE_APP_ID",
  measurementId: "VITE_FIREBASE_MEASUREMENT_ID"
};

function getFirebaseConfig(env) {
  const missingKeys = Object.values(firebaseEnvKeys).filter((key) => !env[key]);
  if (missingKeys.length > 0) {
    throw new Error(`Missing Firebase environment variables: ${missingKeys.join(", ")}`);
  }

  return Object.fromEntries(
    Object.entries(firebaseEnvKeys).map(([configKey, envKey]) => [configKey, env[envKey]])
  );
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), "VITE_");
  const firebaseConfig = getFirebaseConfig(env);
  const serviceWorkerSource = readFileSync(
    resolve(process.cwd(), "src/firebase-messaging-sw.js"),
    "utf8"
  ).replace("__FIREBASE_CONFIG__", JSON.stringify(JSON.stringify(firebaseConfig)));

  return {
    plugins: [{
      name: "firebase-messaging-service-worker",
      configureServer(server) {
        server.middlewares.use((request, response, next) => {
          const pathname = new URL(request.url ?? "/", "http://localhost").pathname;
          if (pathname !== "/firebase-messaging-sw.js") {
            next();
            return;
          }
          response.setHeader("Content-Type", "application/javascript");
          response.end(serviceWorkerSource);
        });
      },
      generateBundle() {
        this.emitFile({
          type: "asset",
          fileName: "firebase-messaging-sw.js",
          source: serviceWorkerSource
        });
      }
    }],
    server: {
      proxy: {
        "/api/v1/auth": "http://localhost:8081",
        "/api/v1/notifications": "http://localhost:8085"
      }
    },
    build: {
      target: "esnext"
    }
  };
});
