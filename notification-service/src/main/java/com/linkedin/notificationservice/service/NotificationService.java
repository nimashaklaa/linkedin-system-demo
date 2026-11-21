package com.linkedin.notificationservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final FirebasePushSender firebasePushSender;

    @KafkaListener(topics = "user.created")
    public void consumeUserCreated(@Payload Map<String, Object> payload) {
        String firstName = requiredString(payload, "firstName");
        firebasePushSender.send(
                requiredString(payload, "userId"),
                "Welcome to LinkedIn!",
                String.format(
                        "Welcome %s! Your account has been created. Start connecting with professionals.",
                        firstName
                )
        );
    }

    @KafkaListener(topics = "user.logged_in")
    public void consumeUserLoggedIn(@Payload Map<String, Object> payload) {
        firebasePushSender.send(
                requiredString(payload, "userId"),
                "You are signed in",
                String.format("Welcome back, %s.", requiredString(payload, "firstName"))
        );
    }

    @KafkaListener(topics = "connection.requested")
    public void consumeConnectionRequest(@Payload Map<String, Object> payload) {
        firebasePushSender.send(
                requiredString(payload, "receiverId"),
                "New connection request",
                String.format("User %s wants to connect with you.", requiredString(payload, "requesterId"))
        );
    }

    @KafkaListener(topics = "connection.accepted")
    public void consumeConnectionAccepted(@Payload Map<String, Object> payload) {
        firebasePushSender.send(
                requiredString(payload, "requesterId"),
                "Connection request accepted",
                String.format(
                        "User %s accepted your connection request. You are now connected!",
                        requiredString(payload, "receiverId")
                )
        );
    }

    @KafkaListener(topics = "post.liked")
    public void consumePostLiked(@Payload Map<String, Object> payload) {
        firebasePushSender.send(
                requiredString(payload, "authorId"),
                "Someone liked your post",
                String.format(
                        "User %s liked your post %s.",
                        requiredString(payload, "userId"),
                        requiredString(payload, "postId")
                )
        );
    }

    @KafkaListener(topics = "comment.created")
    public void consumePostCommented(@Payload Map<String, Object> payload) {
        firebasePushSender.send(
                requiredString(payload, "postAuthorId"),
                "New comment on your post",
                String.format(
                        "User %s commented on your post %s.",
                        requiredString(payload, "authorId"),
                        requiredString(payload, "postId")
                )
        );
    }

    private String requiredString(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return stringValue;
        }
        throw new IllegalArgumentException("Kafka notification event is missing a valid " + key);
    }
}
