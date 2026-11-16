package com.linkedin.userservice.controller;

import com.linkedin.userservice.dto.UserResponse;
import com.linkedin.userservice.dto.UpdateUserProfileRequest;
import com.linkedin.userservice.entity.User;
import com.linkedin.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/user")
@Slf4j
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUserProfile(
            @PathVariable String userId,
            @RequestHeader(value = "X-User-Id") String requestingUserId
    ){
        log.info("Getting user profile for user: {} requested by: {}",userId, requestingUserId);
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PatchMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUserProfile(
            @PathVariable String userId,
            @RequestHeader("X-User-Id") String requestingUserId,
            @RequestBody UpdateUserProfileRequest request
    ){
        if(!requestingUserId.equals(userId)){
            return ResponseEntity.status(403).body(null);
        }
        log.info("Updating user profile for user: {} requested by: {}",userId, requestingUserId);
        return ResponseEntity.ok(userService.updateUserProfile(userId, request));
    }

    @PostMapping("/{targetUserId}/connect")
    public ResponseEntity<String> sendConnectionRequest(
            @PathVariable String targetUserId,
            @RequestHeader("X-User-Id") String requestingUserId
    ){
        log.info("Sending connection request to user: {} requested by: {}",targetUserId, requestingUserId);
        return ResponseEntity.ok(userService.sendConnectionRequest(
                targetUserId,
                requestingUserId
        ));
    }

    @PostMapping("/connection/{connectionId}/accept")
    public ResponseEntity<String> acceptConnection(
            @PathVariable String connectionId,
            @RequestHeader("X-User-Id") String requestingUserId
    ){
        return ResponseEntity.ok(userService.acceptConnectionRequest(connectionId));
    }

    @GetMapping("/{userId}/connections")
    public ResponseEntity<List<UserResponse>> getConnections(
            @PathVariable String userId
    ){
        return ResponseEntity.ok(userService.getConnections(userId));
    }
}
