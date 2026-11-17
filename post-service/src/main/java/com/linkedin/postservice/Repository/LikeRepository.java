package com.linkedin.postservice.Repository;

import com.linkedin.postservice.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LikeRepository extends JpaRepository<Like, String> {
}
