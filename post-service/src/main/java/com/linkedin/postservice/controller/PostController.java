package com.linkedin.postservice.controller;

import com.linkedin.postservice.Service.PostService;
import com.linkedin.postservice.entity.Comment;
import com.linkedin.postservice.entity.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/v1/posts")
@Slf4j
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;


    @PostMapping()
    public ResponseEntity<Post> createPost(
            @RequestParam String authorId,
            @RequestParam String content,
            @RequestParam(required = false)MultipartFile mediaUrl
            ){
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(authorId, content, mediaUrl));
    }

    @GetMapping("/user/{authorId}")
    public ResponseEntity<List<Post>> getPosts(
            @PathVariable String authorId
    ){
        return ResponseEntity.status(HttpStatus.OK).body(postService.getPosts(authorId));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<Post> getPost(
            @PathVariable String postId
    ){
        return ResponseEntity.ok(postService.getPostById(postId));
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<Post> likePost(
            @PathVariable String postId,
            @RequestParam String userId
    ){
        return ResponseEntity.ok(postService.likePost(postId, userId));
    }

    @GetMapping("/liked")
    public ResponseEntity<List<Post>> likedPosts(
            @RequestParam String userId
    ){
        return ResponseEntity.ok(postService.getLikedPosts(userId));
    }

    @PostMapping("/{postId}/comment")
    public ResponseEntity<Comment> addComment(
            @PathVariable String postId,
            @RequestParam String authorId,
            @RequestParam String content
    ){
        return ResponseEntity.ok(postService.addComment(postId, authorId, content));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<String> deletePost(
            @PathVariable String postId,
            @RequestParam String userId
    ){
        return ResponseEntity.ok(postService.deletePost(postId, userId));
    }

}
