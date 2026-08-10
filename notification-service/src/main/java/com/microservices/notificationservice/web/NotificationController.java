package com.microservices.notificationservice.web;

import com.microservices.notificationservice.domain.Notification;
import com.microservices.notificationservice.repository.NotificationRepository;
import com.microservices.notificationservice.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;

    public NotificationController(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @GetMapping
    public List<NotificationResponse> myNotifications(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(principal.userId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getEventId(),
                notification.getEventType(),
                notification.getMessage(),
                notification.getStatus().name(),
                notification.getCreatedAt()
        );
    }

    public record NotificationResponse(
            Long id,
            UUID eventId,
            String eventType,
            String message,
            String status,
            Instant createdAt
    ) {
    }
}
