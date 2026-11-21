package com.linkedin.notificationservice.repository;

import com.linkedin.notificationservice.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {
    List<DeviceToken> findAllByUserId(String userId);

    Optional<DeviceToken> findByToken(String token);

    void deleteByUserIdAndToken(String userId, String token);
}
