package com.linkedin.userservice.service;

import com.linkedin.userservice.dto.AuthResponse;
import com.linkedin.userservice.dto.LoginRequest;
import com.linkedin.userservice.dto.RegisterRequest;
import com.linkedin.userservice.entity.User;
import com.linkedin.userservice.entity.UserRole;
import com.linkedin.userservice.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor

public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${jwt.secret}")
    private String secretKey;
    @Value("${jwt.expiration}")
    private long jwtExpiration;
    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpiration;

    private static final String USER_CREATED_TOPIC = "user.created";
    private static final String USER_LOGGED_IN_TOPIC = "user.logged_in";

    public AuthResponse register(@Valid RegisterRequest registerRequest) {
        log.info("Registering user: {}", registerRequest.getEmail());
        if(userRepository.existsByEmail(registerRequest.getEmail())){
            throw new RuntimeException("Email already registered :"+ registerRequest.getEmail());
        }
        User user = User.
                builder()
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .email(registerRequest.getEmail())
                .headline(registerRequest.getHeadline())
                .location(registerRequest.getLocation())
                .password(bCryptPasswordEncoder.encode(registerRequest.getPassword()))
                .role(UserRole.NORMAL_USER)
                .build();
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getEmail());

        //publish user.created event
        // Search service consume this and indexes user data in elastic search

        Map<String,Object> userCreatedEvent = new HashMap<>();
        userCreatedEvent.put("userId",savedUser.getId());
        userCreatedEvent.put("firstName",savedUser.getFirstName());
        userCreatedEvent.put("lastName",savedUser.getLastName());
        userCreatedEvent.put("email",savedUser.getEmail());
        userCreatedEvent.put("headline",savedUser.getHeadline());
        userCreatedEvent.put("location",savedUser.getLocation());


        kafkaTemplate.send(USER_CREATED_TOPIC,savedUser.getId(),userCreatedEvent);
        log.info("User created event published successfully");

        String token = generateToken(savedUser.getId(), savedUser.getEmail());

        return buildAuthResponse(savedUser, token);
    }

    public AuthResponse login(@Valid LoginRequest loginRequest) {
        log.info("logining user: {}", loginRequest.getEmail());

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(()-> new RuntimeException(
                        "User not found: "+ loginRequest.getEmail()
                ));
        // Bcrypt - compare raw password with stored hash
        if(!bCryptPasswordEncoder.matches(
                loginRequest.getPassword(),
                user.getPassword()
        )){
            throw new RuntimeException("Invalid credentials");
        }
        log.info("User logged in successfully: {}", user.getEmail());

        // Generate JWT token
        String token = generateToken(user.getId(), user.getEmail());
        kafkaTemplate.send(USER_LOGGED_IN_TOPIC, user.getId(), Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "firstName", user.getFirstName() == null ? "there" : user.getFirstName()
        ));
        log.info("User logged-in event published successfully");
        return buildAuthResponse(user, token);

    }



    private AuthResponse buildAuthResponse(User savedUser, String token) {
        return AuthResponse.builder()
                .accessToken(token)
                .refreshToken(generateRefreshToken(savedUser.getId()))
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .build();
    }

    private String generateToken(String userId, String email) {
        return Jwts.builder()
                .claim("userId", userId)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generate a refresh token when it expires
     * Server validate the refresh token and generate a new access token
     * Client stores the refresh token and sends it with every request
     */
    private String generateRefreshToken(String userId) {
        return Jwts.builder()
                .claim("userId", userId)
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }
}
