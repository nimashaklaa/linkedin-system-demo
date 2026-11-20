package com.linkedin.feedbackservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String FEED_KEY_PREFIX = "feed:";


    public List<String> getFeed(String userId, int page, int size) {
        log.info("Getting feed for user: {} page: {} size: {}",userId,page,size);

        String feedKey = FEED_KEY_PREFIX + userId;
        int start = (page) * size;
        int end = start + size-1;

        List<Object> feedItems = redisTemplate.opsForList().range(feedKey, start, end);
        if(feedItems.isEmpty()) {
            log.info("No feed items found for user: {}",userId);
            return new ArrayList<>();
        }

        List<String> result = feedItems.stream()
                .map(Object::toString)
                .toList();
        log.info("Found {} feed items for user: {}",result.size(),userId);
        return result;

    }

    public void clearFeed(String userId) {
        String feedKey = FEED_KEY_PREFIX + userId;
        redisTemplate.delete(feedKey);
        log.info("Feed cleared for user: {}",userId);

    }
}
