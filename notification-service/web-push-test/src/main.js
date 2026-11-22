import { initializeApp } from "firebase/app";
import { getMessaging, getToken, isSupported, onMessage } from "firebase/messaging";

const firebaseConfig = {
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
  appId: import.meta.env.VITE_FIREBASE_APP_ID,
  measurementId: import.meta.env.VITE_FIREBASE_MEASUREMENT_ID
};

const status = document.querySelector("#status");
const app = initializeApp(firebaseConfig);
let activeRegistration = null;

if (await isSupported()) {
  const messaging = getMessaging(app);
  onMessage(messaging, ({ notification }) => {
    const title = notification?.title ?? "New notification";
    const body = notification?.body ?? "";
    status.textContent = `Push received: ${title} — ${body}`;
    if (Notification.permission === "granted") {
      new Notification(title, { body });
    }
  });

  document.querySelector("#login-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const submitButton = form.querySelector('button[type="submit"]');
    submitButton.disabled = true;

    try {
      const email = document.querySelector("#email").value.trim();
      const password = document.querySelector("#password").value;
      const vapidKey = document.querySelector("#vapid-key").value.trim();

      status.textContent = "Requesting browser notification permission…";
      const permission = Notification.permission === "default"
        ? await Notification.requestPermission()
        : Notification.permission;

      status.textContent = "Signing in…";
      const loginResponse = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password })
      });
      if (!loginResponse.ok) {
        throw new Error(`Sign-in failed (HTTP ${loginResponse.status}): ${await loginResponse.text()}`);
      }
      const { userId } = await loginResponse.json();
      if (!userId) {
        throw new Error("Sign-in succeeded but the User Service response did not contain a user ID.");
      }
      document.querySelector("#password").value = "";

      if (permission !== "granted") {
        await unregisterStoredDevice();
        activeRegistration = null;
        status.textContent = `Signed in as ${email}, but browser notification permission was not granted. Push notifications are disabled for this browser.`;
        return;
      }
      await navigator.serviceWorker.register("/firebase-messaging-sw.js", { scope: "/" });
      const registration = await navigator.serviceWorker.ready;
      if (!registration.active) {
        throw new Error("The Firebase service worker did not activate. Check the browser console for its registration error.");
      }

      const token = await getToken(messaging, {
        vapidKey,
        serviceWorkerRegistration: registration
      });
      if (!token) {
        throw new Error("Firebase did not return a registration token.");
      }

      await syncDeviceToken(userId, token);
      activeRegistration = { userId, messaging, registration, vapidKey };
      status.textContent = `Signed in as ${email}; this browser is registered for push notifications.`;
    } catch (error) {
      status.textContent = `Could not complete sign-in and notification setup: ${error.message}`;
    } finally {
      submitButton.disabled = false;
    }
  });

  document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible" && activeRegistration) {
      refreshDeviceRegistration();
    }
  });
  window.setInterval(() => {
    if (activeRegistration) {
      refreshDeviceRegistration();
    }
  }, 12 * 60 * 60 * 1000);
} else {
  status.textContent = "This browser does not support Firebase Cloud Messaging.";
}

async function refreshDeviceRegistration() {
  try {
    const { userId, messaging, registration, vapidKey } = activeRegistration;
    const token = await getToken(messaging, {
      vapidKey,
      serviceWorkerRegistration: registration
    });
    if (!token) {
      throw new Error("Firebase did not return a registration token.");
    }
    await syncDeviceToken(userId, token);
    status.textContent = "Push registration is up to date.";
  } catch (error) {
    status.textContent = `Could not refresh push registration: ${error.message}`;
  }
}

async function syncDeviceToken(userId, token) {
  const storageKey = "fcm-device-registration";
  const previous = JSON.parse(localStorage.getItem(storageKey) ?? "null");
  if (previous && (previous.token !== token || previous.userId !== userId)) {
    const deleteResponse = await fetch(`/api/v1/notifications/devices/${encodeURIComponent(previous.userId)}`, {
      method: "DELETE",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ token: previous.token })
    });
    if (!deleteResponse.ok) {
      throw new Error(`Could not remove the previous device registration (HTTP ${deleteResponse.status}).`);
    }
  }

  const response = await fetch(`/api/v1/notifications/devices/${encodeURIComponent(userId)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token })
  });
  if (!response.ok) {
    throw new Error(`Device registration failed (HTTP ${response.status}): ${await response.text()}`);
  }
  localStorage.setItem(storageKey, JSON.stringify({ userId, token }));
}

async function unregisterStoredDevice() {
  const storageKey = "fcm-device-registration";
  const previous = JSON.parse(localStorage.getItem(storageKey) ?? "null");
  if (!previous) {
    return;
  }

  const response = await fetch(`/api/v1/notifications/devices/${encodeURIComponent(previous.userId)}`, {
    method: "DELETE",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ token: previous.token })
  });
  if (!response.ok) {
    throw new Error(`Could not remove the previous device registration (HTTP ${response.status}).`);
  }
  localStorage.removeItem(storageKey);
}
