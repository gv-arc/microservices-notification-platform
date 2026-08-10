package com.microservices.common.event;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserEvent(
        UUID eventId,
        String eventType,
        Long userId,
        String email,
        String fullName,
        Instant occurredAt
) {
    public static UserEvent created(Long userId, String email, String fullName) {
        return new UserEvent(
                UUID.randomUUID(),
                EventTypes.USER_CREATED,
                userId,
                email,
                fullName,
                Instant.now()
        );
    }

    public static UserEvent updated(Long userId, String email, String fullName) {
        return new UserEvent(
                UUID.randomUUID(),
                EventTypes.USER_UPDATED,
                userId,
                email,
                fullName,
                Instant.now()
        );
    }
}
