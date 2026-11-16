package com.linkedin.userservice.repository;

import com.linkedin.userservice.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(@NotBlank(message = "Email is Required") @Email(message = "Email is not valid") String email);
}
