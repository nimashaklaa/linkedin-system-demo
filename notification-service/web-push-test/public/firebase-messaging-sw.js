importScripts("https://www.gstatic.com/firebasejs/10.13.2/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.13.2/firebase-messaging-compat.js");

firebase.initializeApp({
  apiKey: "AIzaSyDNXBLv2yaewo_02Euqqv8fu-S2Gdb9Z6w",
  authDomain: "push-notification-90b72.firebaseapp.com",
  projectId: "push-notification-90b72",
  storageBucket: "push-notification-90b72.firebasestorage.app",
  messagingSenderId: "891347951307",
  appId: "1:891347951307:web:338ab468d858e5d055c54e",
  measurementId: "G-9K1P4FPX9V"
});

firebase.messaging().onBackgroundMessage(({ notification }) => {
  if (notification) {
    self.registration.showNotification(notification.title ?? "New notification", {
      body: notification.body ?? ""
    });
  }
});
