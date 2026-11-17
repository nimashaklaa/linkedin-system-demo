package com.linkedin.postservice.Service;

import com.linkedin.postservice.Repository.CommentRepository;
import com.linkedin.postservice.Repository.LikeRepository;
import com.linkedin.postservice.Repository.PostRepository;
import com.linkedin.postservice.entity.Comment;
import com.linkedin.postservice.entity.Like;
import com.linkedin.postservice.entity.Post;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final PostRepository postRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final S3Service s3Service;

    private static final String POST_CREATED_TOPIC = "post.created";
    private static final String POST_LIKED_TOPIC="post.liked";

    public Post createPost(String authorId, String content, MultipartFile media) throws IOException {
        log.info("Creating post for user: {} with content: {}", authorId, content);

        String mediaUrl = null;

        if(media!=null && !media.isEmpty()) {
            mediaUrl = s3Service.uploadFile(media,
                    "posts/"+authorId);
        }

        Post savedPost = postRepository.save(Post.builder()
                .authorId(authorId)
                .content(content)
                .mediaUrl(mediaUrl)
                .build());
        log.info("Post created successfully: {}", savedPost.getId());

        //publish post.created event
        Map<String,Object> postCreatedEvent = new HashMap<>();
        postCreatedEvent.put("postId",savedPost.getId());
        postCreatedEvent.put("authorId",savedPost.getAuthorId());
        postCreatedEvent.put("content",savedPost.getContent());
        postCreatedEvent.put("mediaUrl",savedPost.getMediaUrl());
        postCreatedEvent.put("createdAt", savedPost.getCreatedAt().toString());

        kafkaTemplate.send(POST_CREATED_TOPIC,savedPost.getId(),postCreatedEvent);
        log.info("Post created event published successfully");
        return savedPost;
    }


    public List<Post> getPosts(String authorId) {
        log.info("Getting posts for user: {}", authorId);
        List<Post> posts = postRepository.findAllPostsByAuthorIdOrderByCreatedAtDesc(authorId);
        log.info("Posts found: {}", posts.size());
        return posts;
    }

    public Post getPostById(String postId) {
        return postRepository.findById(postId).orElseThrow(()-> new RuntimeException("Post not found"));
    }

    @Transactional
    public Like likePost(String postId, String userId) {
        Post post = postRepository.findById(postId).orElseThrow(()-> new RuntimeException("Post not found"));
        Like existingLike = likeRepository.findByUserIdAndPostId(userId,postId);
        if(existingLike!=null) {
            likeRepository.delete(existingLike);
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
            log.info("Post unliked successfully: {}", post.getId());
            return null;
        }
        try{
            Like savedLike = likeRepository.save(Like.builder()
                    .postId(postId)
                    .userId(userId)
                    .build());
            post.setLikeCount(post.getLikeCount()+1);
            postRepository.save(post);
            log.info("Like post for user: {} with id: {}", userId, savedLike.getId());

            Map<String,Object> postLikedEvent = new HashMap<>();
            postLikedEvent.put("postId",savedLike.getId());
            postLikedEvent.put("userId", savedLike.getUserId());

            kafkaTemplate.send(POST_CREATED_TOPIC,savedLike.getId(),postLikedEvent);
            log.info("Like created event published successfully");
            return savedLike;
        }catch (DataIntegrityViolationException e) {
            log.warn("Concurrent like attempt detected for user: {} on post: {}", userId, postId);
            return likeRepository.findByUserIdAndPostId(userId, postId);
        }
    }

    public List<Post> getLikedPosts(String userId) {
        List<Like> liked_posts = likeRepository.findAllByUserId(userId);
        if(liked_posts.isEmpty()){
            return new ArrayList<>();
        }
        List<String> postsIds = liked_posts.stream()
                .map(Like::getPostId)
                .toList();
        List<Post> posts = postRepository.findAllById(postsIds);
        log.info("Found {} liked posts for user: {}", posts.size(), userId);
        return posts;
    }


    public Comment addComment(String postId, String authorId, String content) {
        Comment savedComment = commentRepository.save(Comment.builder()
                .postId(postId)
                .authorId(authorId)
                .content(content)
                .build());
        log.info("Comment created successfully: {}", savedComment.getId());
        return savedComment;
    }

    @Transactional
    public String deletePost(String postId, String userId) {
        Post post = postRepository.findById(postId).orElseThrow(()-> new RuntimeException("Post not found"));

        if(!post.getAuthorId().equals(userId)){
            throw new RuntimeException("Unauthorized: You can only delete your own posts");
        }
        likeRepository.deleteAllByPostId(postId);
        commentRepository.deleteAllByPostId(postId);
        postRepository.deleteById(postId);

        log.info("Post with id: {} deleted successfully by user: {}", postId, userId);
        return "Post deleted successfully";
    }

    public Page<Comment> getComments(String postId, Pageable pageable) {
        return commentRepository.findAllByPostId(postId, pageable);
    }
}
