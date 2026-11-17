package com.linkedin.postservice.Repository;

import com.linkedin.postservice.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;;

public interface PostRepository extends JpaRepository<Post, String> {
    List<Post> findAllPostsByAuthorIdOrderByCreatedAtDesc(String authorId);
}
