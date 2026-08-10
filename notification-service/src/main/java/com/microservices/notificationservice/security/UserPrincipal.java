package com.microservices.notificationservice.security;

public record UserPrincipal(String email, Long userId) {
}
