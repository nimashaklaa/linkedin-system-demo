package com.linkedin.userservice.service;

import com.linkedin.userservice.dto.UserResponse;
import com.linkedin.userservice.dto.UpdateUserProfileRequest;
import com.linkedin.userservice.entity.Connection;
import com.linkedin.userservice.entity.ConnectionStatus;
import com.linkedin.userservice.entity.User;
import com.linkedin.userservice.repository.ConnectionRepository;
import com.linkedin.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor

public class UserService {

    private final UserRepository userRepository;
    private final ConnectionRepository connectionRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CONNECTION_REQUEST_TOPIC = "connection.requested";
    private static final String CONNECTION_ACCEPTED_TOPIC = "connection.accepted";

    public UserResponse getUserProfile(String userId) {
        log.info("Getting user profile for user: {}", userId);
        User user = userRepository.findById(userId).orElseThrow(()-> new RuntimeException("User not found"));
        return buildUserResponse(user);
    }


    public UserResponse updateUserProfile(String userId, UpdateUserProfileRequest request) {
        log.info("Updating user profile for user: {}", userId);
        User existingUser = userRepository.findById(userId).orElseThrow(()-> new RuntimeException("User not found"));

        if (request.getFirstName() != null) existingUser.setFirstName(request.getFirstName());
        if (request.getLastName() != null) existingUser.setLastName(request.getLastName());
        if (request.getHeadline() != null) existingUser.setHeadline(request.getHeadline());
        if (request.getLocation() != null) existingUser.setLocation(request.getLocation());
        if (request.getAbout() != null) existingUser.setAbout(request.getAbout());
        if (request.getProfilePhotoUrl() != null) existingUser.setProfilePhotoUrl(request.getProfilePhotoUrl());
        if (request.getCoverPhotoUrl() != null) existingUser.setCoverPhotoUrl(request.getCoverPhotoUrl());
        if (request.getSkills() != null) existingUser.setSkills(new ArrayList<>(request.getSkills()));
        userRepository.save(existingUser);
        

        return buildUserResponse(existingUser);
    }

    private UserResponse buildUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .headline(user.getHeadline())
                .location(user.getLocation())
                .about(user.getAbout())
                .profilePhotoUrl(user.getProfilePhotoUrl())
                .coverPhotoUrl(user.getCoverPhotoUrl())
                .role(user.getRole())
                .skills(user.getSkills())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public String sendConnectionRequest(String targetUserId, String requestingUserId) {
        if (connectionRepository.existsByRequesterIdAndReceiverId(
                requestingUserId,targetUserId
        )){
            throw new RuntimeException("Connection already exists");
        }
        Connection connection = Connection.builder()
                .requesterId(requestingUserId)
                .receiverId(targetUserId)
                .status(ConnectionStatus.PENDING)
                .build();
        connectionRepository.save(connection);

        Map<String,Object> connectionRequestedEvent = new HashMap<>();
        connectionRequestedEvent.put("requesterId",requestingUserId);
        connectionRequestedEvent.put("receiverId",targetUserId);

        kafkaTemplate.send(CONNECTION_REQUEST_TOPIC, requestingUserId,connectionRequestedEvent);
        log.info("Connection request sent: {} -> {}",requestingUserId, targetUserId);
        return "Connection request sent";
    }

    public String acceptConnectionRequest(String connectionId) {
        Connection connection = connectionRepository.findById(connectionId).orElseThrow(
                ()-> new RuntimeException("Connection not found")
        );
        connection.setStatus(ConnectionStatus.CONNECTED);
        connectionRepository.save(connection);

        Map<String,Object> connectionAcceptedEvent = new HashMap<>();
        connectionAcceptedEvent.put("requesterId",connection.getRequesterId());
        connectionAcceptedEvent.put("receiverId",connection.getReceiverId());

        kafkaTemplate.send(CONNECTION_ACCEPTED_TOPIC, connectionId,connectionAcceptedEvent);
        log.info("Connection accepted: {} -> {}",connection.getRequesterId(), connection.getReceiverId());
        return "Connection accepted";
    }


    public List<UserResponse> getConnections(String userId) {
        List<Connection> connections = connectionRepository.findByRequesterIdAndStatus(userId,ConnectionStatus.CONNECTED);

        return connections.stream()
                .map(connection -> getUserProfile(connection.getReceiverId())).collect(Collectors.toList());
    }
}
