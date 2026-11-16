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
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
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
    private static final String PROFILE_UPDATED_TOPIC = "profile.updated";
    private final S3Service s3Service;

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
        User savedUser = userRepository.save(existingUser);

        UserResponse response = buildUserResponse(savedUser);
        kafkaTemplate.send(PROFILE_UPDATED_TOPIC, savedUser.getId(), response);
        log.info("Profile updated event sent for user: {}", savedUser.getId());

        return response;
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
                .profilePhotoUrl(toPhotoUrl(user.getProfilePhotoUrl()))
                .coverPhotoUrl(toPhotoUrl(user.getCoverPhotoUrl()))
                .role(user.getRole())
                .skills(user.getSkills())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String toPhotoUrl(String photoKey) {
        if (photoKey != null && photoKey.startsWith("profiles/")) {
            return s3Service.getFileUrl(photoKey);
        }
        return photoKey;
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

    public String acceptConnectionRequest(String connectionId, String requestingUserId) {
        Connection connection = connectionRepository.findById(connectionId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Connection not found")
        );
        if (!connection.getReceiverId().equals(requestingUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the recipient can accept this connection");
        }
        if (connection.getStatus() != ConnectionStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Connection request is not pending");
        }

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

    public UserResponse uploadProfilePhoto(String userId, MultipartFile file) throws IOException {
        return uploadUserPhoto(userId, file, "avatar", true);
    }

    public UserResponse uploadCoverPhoto(String userId, MultipartFile file) throws IOException {
        return uploadUserPhoto(userId, file, "cover", false);
    }

    private UserResponse uploadUserPhoto(
            String userId,
            MultipartFile file,
            String imageType,
            boolean profilePhoto
    ) throws IOException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
        String photoKey = s3Service.uploadFile(file, "profiles/" + userId + "/" + imageType);

        if (profilePhoto) {
            user.setProfilePhotoUrl(photoKey);
        } else {
            user.setCoverPhotoUrl(photoKey);
        }
        User savedUser = userRepository.save(user);
        return buildUserResponse(savedUser);
    }
}
