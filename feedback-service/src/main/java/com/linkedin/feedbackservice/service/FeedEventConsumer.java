package com.linkedin.feedbackservice.service;

import com.linkedin.feedbackservice.cilent.UserServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedEventConsumer {
    private final UserServiceClient userServiceClient;
    private static final String FEED_KEY_PREFIX = "feed:";
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${feed.max-size}")
    private int maxFeedSize;


    @KafkaListener(topics = "post.created")
    public void consumePostCreatedEvent(
            @Payload Map<String, Object> payload
    ) {
        try{
            String postId = (String) payload.get("postId");
            String authorId = (String) payload.get("authorId");
            log.info("Received feed event for post: {} by user: {}", postId, authorId);

            // Get all connections of the user and send notification to them
            List<Map<String, Object>> connections = userServiceClient.getConnections(authorId);

            //Push post to each connection's feed
            for (Map<String, Object> connection : connections) {
                String connectionId =(String) connection.get("id");
                String feedKey = FEED_KEY_PREFIX+ connectionId;

                // Add post to feed(as sorted set - score+timestamp)
                redisTemplate.opsForList().leftPush(feedKey, postId);
                redisTemplate.opsForList().trim(feedKey, 0, maxFeedSize-1);

                log.info("Post {} added to feed of user: {}", postId, connectionId);
            }
            String authorFeedKey = FEED_KEY_PREFIX+authorId;
            redisTemplate.opsForList().leftPush(authorFeedKey, postId);
            redisTemplate.opsForList().trim(authorFeedKey, 0, maxFeedSize-1);

        }catch (Exception e){
            log.error("Error processing feed event: {}", e.getMessage());
        }
    }
}
