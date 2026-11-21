import { initializeApp } from "firebase/app";
import {
  deleteToken,
  getMessaging,
  getToken,
  isSupported,
  onMessage
} from "firebase/messaging";

const firebaseConfig = {
  apiKey: "AIzaSyDNXBLv2yaewo_02Euqqv8fu-S2Gdb9Z6w",
  authDomain: "push-notification-90b72.firebaseapp.com",
  projectId: "push-notification-90b72",
  storageBucket: "push-notification-90b72.firebasestorage.app",
  messagingSenderId: "891347951307",
  appId: "1:891347951307:web:338ab468d858e5d055c54e",
  measurementId: "G-9K1P4FPX9V"
};

const status = document.querySelector("#status");
const tokenField = document.querySelector("#token");
const app = initializeApp(firebaseConfig);

if (await isSupported()) {
  const messaging = getMessaging(app);
  onMessage(messaging, ({ notification }) => {
    status.textContent = `Push received while this page is open: ${notification?.title ?? "(no title)"} — ${notification?.body ?? ""}`;
  });

  document.querySelector("#invalidate-token").addEventListener("click", async () => {
    status.textContent = "Invalidating this browser's current FCM token…";
    try {
      const vapidKey = document.querySelector("#vapid-key").value.trim();
      if (!vapidKey) {
        throw new Error("Enter the VAPID public key first.");
      }

      await navigator.serviceWorker.register("/firebase-messaging-sw.js", { scope: "/" });
      const registration = await navigator.serviceWorker.ready;
      const existingToken = await getToken(messaging, {
        vapidKey,
        serviceWorkerRegistration: registration
      });

      if (existingToken) {
        const userId = document.querySelector("#user-id").value.trim();
        const response = await fetch(`${getServiceUrl()}/api/v1/notifications/devices/${encodeURIComponent(userId)}`, {
          method: "DELETE",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ token: existingToken })
        });
        if (!response.ok) {
          throw new Error(`Could not remove the old token from Notification Service (HTTP ${response.status}).`);
        }
      }

      await deleteToken(messaging);
      tokenField.value = "";
      status.textContent = "Current token invalidated. Submit the form to generate and register a fresh token.";
    } catch (error) {
      status.textContent = `Could not invalidate token: ${error.message}`;
    }
  });

  document.querySelector("#register-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    status.textContent = "Requesting notification permission…";

    try {
      const permission = await Notification.requestPermission();
      if (permission !== "granted") {
        throw new Error("Notification permission was not granted.");
      }

      await navigator.serviceWorker.register("/firebase-messaging-sw.js", { scope: "/" });
      const registration = await navigator.serviceWorker.ready;
      if (!registration.active) {
        throw new Error("The Firebase service worker did not activate. Check the browser console for its registration error.");
      }

      const vapidKey = document.querySelector("#vapid-key").value.trim();
      const token = await getToken(messaging, {
        vapidKey,
        serviceWorkerRegistration: registration
      });
      if (!token) {
        throw new Error("Firebase did not return a registration token.");
      }
      tokenField.value = token;

      const userId = document.querySelector("#user-id").value.trim();
      const response = await fetch(`${getServiceUrl()}/api/v1/notifications/devices/${encodeURIComponent(userId)}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token })
      });
      if (!response.ok) {
        throw new Error(`Notification Service returned HTTP ${response.status}: ${await response.text()}`);
      }
      status.textContent = `Browser token registered successfully (HTTP ${response.status}).`;
    } catch (error) {
      status.textContent = `Could not register browser: ${error.message}`;
    }
  });
} else {
  status.textContent = "This browser does not support Firebase Cloud Messaging.";
}

function getServiceUrl() {
  return document.querySelector("#service-url").value.trim().replace(/\/+$/, "");
}
