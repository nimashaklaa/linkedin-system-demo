package com.linkedin.postservice.Service;

import com.linkedin.postservice.Repository.PostRepository;
import com.linkedin.postservice.entity.Comment;
import com.linkedin.postservice.entity.Like;
import com.linkedin.postservice.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String POST_CREATED_TOPIC = "post-created";
    private final PostRepository postRepository;

    public Post createPost(String authorId, String content, MultipartFile mediaUrl) {
        log.info("Creating post for user: {} with content: {}", authorId, content);

        Post post = Post.builder()
                .authorId(authorId)
                .content(content)
                .mediaUrl(mediaUrl.getOriginalFilename())
                .build();
        Post savedPost = postRepository.save(post);
        log.info("Post created successfully: {}", savedPost.getId());

        //publish post.created event
        Map<String,Object> postCreatedEvent = new HashMap<>();
        postCreatedEvent.put("postId",savedPost.getId());
        postCreatedEvent.put("authorId",savedPost.getAuthorId());
        postCreatedEvent.put("content",savedPost.getContent());
        postCreatedEvent.put("mediaUrl",savedPost.getMediaUrl());

        kafkaTemplate.send(POST_CREATED_TOPIC,savedPost.getId(),postCreatedEvent);
        log.info("Post created event published successfully");
        return savedPost;
    }


    public List<Post> getPosts(String authorId) {
        log.info("Getting posts for user: {}", authorId);
        List<Post> posts = postRepository.findAllPostsByAuthorId(authorId);
        log.info("Posts found: {}", posts.size());
        return posts;
    }

    public Post getPostById(String postId) {
        return postRepository.findById(postId).orElseThrow(()-> new RuntimeException("Post not found"));
    }

    public List<Post> getLikedPosts(String userId) {

    }

    public Like likePost(String postId, String userId) {

    }

    public Comment addComment(String postId, String authorId, String content) {
    }

    public String deletePost(String postId, String userId) {
    }

    public List<Comment> getComments(String postId) {
    }
}
