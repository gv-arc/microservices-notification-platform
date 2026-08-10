package com.microservices.common.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        Instant createdAt
) {
}
