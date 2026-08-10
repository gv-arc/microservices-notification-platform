package com.microservices.userservice.service;

import com.microservices.common.dto.AuthResponse;
import com.microservices.common.dto.LoginRequest;
import com.microservices.common.dto.RegisterUserRequest;
import com.microservices.common.dto.UserResponse;
import com.microservices.common.event.UserEvent;
import com.microservices.userservice.domain.User;
import com.microservices.userservice.messaging.OutboxRelayService;
import com.microservices.userservice.repository.UserRepository;
import com.microservices.userservice.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OutboxRelayService outboxRelayService;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OutboxRelayService outboxRelayService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.outboxRelayService = outboxRelayService;
    }

    @Transactional
    public AuthResponse register(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        User user = new User();
        user.setFullName(request.fullName());
        user.setEmail(request.email().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        outboxRelayService.enqueue(UserEvent.created(saved.getId(), saved.getEmail(), saved.getFullName()));

        String token = jwtService.generateToken(saved.getEmail(), saved.getId());
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds());
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase())
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getEmail(), user.getId());
        return AuthResponse.bearer(token, jwtService.getExpirationSeconds());
    }

    @Transactional(readOnly = true)
    public UserResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(email));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(String email, String fullName) {
        User user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new UserNotFoundException(email));
        user.setFullName(fullName);
        User saved = userRepository.save(user);
        outboxRelayService.enqueue(UserEvent.updated(saved.getId(), saved.getEmail(), saved.getFullName()));
        return toResponse(saved);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt());
    }

    public static class DuplicateEmailException extends RuntimeException {
        public DuplicateEmailException(String email) {
            super("Email already registered: " + email);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid email or password");
        }
    }

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String email) {
            super("User not found: " + email);
        }
    }
}
