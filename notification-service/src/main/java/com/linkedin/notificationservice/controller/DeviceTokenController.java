package com.linkedin.notificationservice.controller;

import com.linkedin.notificationservice.dto.DeviceTokenRequest;
import com.linkedin.notificationservice.service.DeviceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications/devices/{userId}")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"})
@RequiredArgsConstructor
public class DeviceTokenController {
    private final DeviceTokenService deviceTokenService;

    @PostMapping
    public ResponseEntity<Void> registerDevice(
            @PathVariable String userId,
            @Valid @RequestBody DeviceTokenRequest request
    ) {
        deviceTokenService.register(userId, request.token());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unregisterDevice(
            @PathVariable String userId,
            @Valid @RequestBody DeviceTokenRequest request
    ) {
        deviceTokenService.unregister(userId, request.token());
        return ResponseEntity.noContent().build();
    }
}
