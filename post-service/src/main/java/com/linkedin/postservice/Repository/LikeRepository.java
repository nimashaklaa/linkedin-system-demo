package com.linkedin.postservice.Repository;

import com.linkedin.postservice.entity.Like;
import com.linkedin.postservice.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LikeRepository extends JpaRepository<Like, String> {
    List<Like> findAllByUserId(String userId);

    void deleteAllByPostId(String postId);
}
