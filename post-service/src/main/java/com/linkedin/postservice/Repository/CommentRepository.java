package com.linkedin.postservice.Repository;

import com.linkedin.postservice.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, String> {
    void deleteAllByPostId(String postId);

    Page<Comment> findAllByPostId(String postId, Pageable pageable);
}
