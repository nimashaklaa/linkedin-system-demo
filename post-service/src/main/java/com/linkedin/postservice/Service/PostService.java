package com.linkedin.postservice.Service;

import com.linkedin.postservice.entity.Comment;
import com.linkedin.postservice.entity.Post;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
public class PostService {
    
    public Post createPost(String authorId, String content, MultipartFile mediaUrl) {
    }


    public List<Post> getPosts(String authorId) {
        return null;
    }

    public Post getPostById(String postId) {
    }

    public List<Post> getLikedPosts(String userId) {
    }

    public Post likePost(String postId, String userId) {
    }

    public Comment addComment(String postId, String authorId, String content) {
    }

    public String deletePost(String postId, String userId) {
    }
}
