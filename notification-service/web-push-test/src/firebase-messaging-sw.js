import firebase from "firebase/compat/app";

importScripts("https://www.gstatic.com/firebasejs/10.13.2/firebase-app-compat.js");
importScripts("https://www.gstatic.com/firebasejs/10.13.2/firebase-messaging-compat.js");

const firebaseConfig = JSON.parse("__FIREBASE_CONFIG__");
firebase.initializeApp(firebaseConfig);

firebase.messaging().onBackgroundMessage(({ notification }) => {
  if (notification) {
    self.registration.showNotification(notification.title ?? "New notification", {
      body: notification.body ?? ""
    });
  }
});
