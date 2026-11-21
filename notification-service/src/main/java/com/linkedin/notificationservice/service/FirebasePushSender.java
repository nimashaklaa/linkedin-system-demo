package com.linkedin.notificationservice.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.linkedin.notificationservice.entity.DeviceToken;
import com.linkedin.notificationservice.repository.DeviceTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class FirebasePushSender {
    private static final int FCM_MAX_MULTICAST_SIZE = 500;

    private final DeviceTokenRepository deviceTokenRepository;
    private final String projectId;
    private volatile FirebaseMessaging firebaseMessaging;

    public FirebasePushSender(
            DeviceTokenRepository deviceTokenRepository,
            @Value("${firebase.project-id:}") String projectId
    ) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.projectId = projectId;
    }

    public void send(String userId, String title, String message) {
        List<DeviceToken> devices = deviceTokenRepository.findAllByUserId(userId);
        if (devices.isEmpty()) {
            log.info("No registered push devices for user {}", userId);
            return;
        }

        List<String> tokens = devices.stream().map(DeviceToken::getToken).toList();
        int failures = 0;
        for (int start = 0; start < tokens.size(); start += FCM_MAX_MULTICAST_SIZE) {
            List<String> batch = tokens.subList(start, Math.min(start + FCM_MAX_MULTICAST_SIZE, tokens.size()));
            try {
                MulticastMessage push = MulticastMessage.builder()
                        .addAllTokens(batch)
                        .setNotification(Notification.builder().setTitle(title).setBody(message).build())
                        .build();
                BatchResponse response = getFirebaseMessaging().sendEachForMulticast(push);
                failures += response.getFailureCount();
                response.getResponses().stream()
                        .filter(sendResponse -> !sendResponse.isSuccessful())
                        .forEach(sendResponse -> log.error(
                                "FCM push failed for user {}: {}",
                                userId,
                                sendResponse.getException().getMessage()
                        ));
            } catch (FirebaseMessagingException e) {
                log.error("FCM push request failed for user {}", userId, e);
                throw new IllegalStateException("Could not send push notification to user " + userId, e);
            }
        }
        if (failures > 0) {
            throw new IllegalStateException("FCM failed to deliver to " + failures + " device(s) for user " + userId);
        }
        log.info("Push notification sent to {} device(s) for user {}", tokens.size(), userId);
    }

    private FirebaseMessaging getFirebaseMessaging() {
        if (firebaseMessaging == null) {
            synchronized (this) {
                if (firebaseMessaging == null) {
                    firebaseMessaging = initializeFirebaseMessaging();
                }
            }
        }
        return firebaseMessaging;
    }

    private FirebaseMessaging initializeFirebaseMessaging() {
        try {
            FirebaseApp app = FirebaseApp.getApps().stream()
                    .filter(existingApp -> FirebaseApp.DEFAULT_APP_NAME.equals(existingApp.getName()))
                    .findFirst()
                    .orElseGet(() -> {
                        try {
                            FirebaseOptions.Builder options = FirebaseOptions.builder()
                                    .setCredentials(GoogleCredentials.getApplicationDefault());
                            if (!projectId.isBlank()) {
                                options.setProjectId(projectId);
                            }
                            return FirebaseApp.initializeApp(options.build());
                        } catch (IOException e) {
                            throw new IllegalStateException("Could not load Firebase application credentials", e);
                        }
                    });
            return FirebaseMessaging.getInstance(app);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not initialize Firebase Cloud Messaging", e);
        }
    }
}
