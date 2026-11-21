package com.linkedin.notificationservice.service;

import com.linkedin.notificationservice.entity.DeviceToken;
import com.linkedin.notificationservice.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeviceTokenService {
    private final DeviceTokenRepository deviceTokenRepository;

    @Transactional
    public void register(String userId, String token) {
        DeviceToken deviceToken = deviceTokenRepository.findByToken(token)
                .orElseGet(() -> DeviceToken.builder().token(token).build());
        deviceToken.setUserId(userId);
        deviceTokenRepository.save(deviceToken);
    }

    @Transactional
    public void unregister(String userId, String token) {
        deviceTokenRepository.deleteByUserIdAndToken(userId, token);
    }
}
