package com.linkedin.userservice.repository;

import com.linkedin.userservice.entity.Connection;
import com.linkedin.userservice.entity.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConnectionRepository extends JpaRepository<Connection,String> {
    boolean existsByRequesterIdAndReceiverId(String requestingUserId, String targetUserId);

    List<Connection> findByRequesterIdAndStatus(String userId, ConnectionStatus connectionStatus);
}
