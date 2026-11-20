package com.linkedin.feedbackservice.cilent;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

// OpenFeign lets you define a simple Java interface, and Spring automatically generates the implementation for you.
// Tells Spring Boot that this interface is an HTTP client that communicates with your user-service
@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserServiceClient {
    @GetMapping("/api/v1/user/{userId}/connections")
    List<Map<String, Object>> getConnections(
            @PathVariable String userId
    );
}
