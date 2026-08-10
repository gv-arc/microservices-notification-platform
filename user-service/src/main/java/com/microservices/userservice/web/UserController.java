package com.microservices.userservice.web;

import com.microservices.common.dto.AuthResponse;
import com.microservices.common.dto.LoginRequest;
import com.microservices.common.dto.RegisterUserRequest;
import com.microservices.common.dto.UserResponse;
import com.microservices.userservice.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/users/me")
    public UserResponse currentUser(Authentication authentication) {
        return userService.getByEmail(authentication.getName());
    }

    @PutMapping("/users/me")
    public UserResponse updateProfile(Authentication authentication, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authentication.getName(), request.fullName());
    }

    public record UpdateProfileRequest(@jakarta.validation.constraints.NotBlank String fullName) {
    }
}
