package com.linkedin.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    @KafkaListener(topics = "user.created")
    public void consumeUserCreated(
            @Payload Map<String ,Object> payload
    ){
        try{
            String userId = (String) payload.get("userId");
            String firstName = (String) payload.get("firstName");

            sendNotification(userId, "Welcome to LinkedIn!", String.format(
                    "Welcome %s Your account has been created"+
                            "Start connecting with professionals", firstName
            ));
        }catch (Exception e){
            log.error("Error sending notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "connection.request")
    public void consumeConnectionRequest(
            @Payload Map<String ,Object> payload
    ){
        try{
            String senderId = (String) payload.get("senderId");
            String receiverId = (String) payload.get("receiverId");
            sendNotification(receiverId, "New connectionRequest", String.format(
                    "User %s want to connect with you", senderId
            ));
        }catch (Exception e){
            log.error("Error sending connection request notification: {}", e.getMessage());
        }
    }
    @KafkaListener(topics = "connection.accepted")
    public void consumeConnectionAccepted(
            @Payload Map<String ,Object> payload
    ){
        try{
            String requesterId = (String) payload.get("requesterId");
            String receiverId = (String) payload.get("receiverId");
            sendNotification(receiverId, "New connectionRequest", String.format(
                    "User %s accepted your connection request"+
                            "You are now connected!"
                    , requesterId
            ));
        }catch (Exception e){
            log.error("Error sending connection request notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "post.liked")
    public void consumePostLiked(
            @Payload Map<String ,Object> payload
    ){
        try{
            String postId = (String) payload.get("postId");
            String userId = (String) payload.get("userId");
            String authorId = (String) payload.get("authorId");
            sendNotification(authorId, "Someone liked your post", String.format(
                    "User %s liked your post %s"
                    ,userId,postId
            ));
        }catch (Exception e){
            log.error("Error sending post liked notification: {}", e.getMessage());
        }
    }
    @KafkaListener(topics = "comment.created")
    public void consumePostCommented(
            @Payload Map<String ,Object> payload
    ){
        try{
            String postId = (String) payload.get("postId");
            String commenterId = (String) payload.get("authorId");
            String postAuthorId = (String) payload.get("postAuthorId");
            sendNotification(postAuthorId, "New comment on your post", String.format(
                    "User %s commented your post %s"
                    ,commenterId,postId
            ));
        }catch (Exception e){
            log.error("Error sending post comment notification: {}", e.getMessage());
        }
    }

    private void sendNotification(String userId, String title, String message) {
    }
}
