package com.microservices.notificationservice.service;

import com.microservices.common.event.EventTypes;
import com.microservices.common.event.UserEvent;
import com.microservices.notificationservice.domain.Notification;
import com.microservices.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationProcessor.class);

    private final NotificationRepository notificationRepository;

    public NotificationProcessor(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void process(UserEvent event) {
        if (notificationRepository.existsByEventId(event.eventId())) {
            log.debug("Skipping duplicate event {}", event.eventId());
            return;
        }

        Notification notification = new Notification();
        notification.setEventId(event.eventId());
        notification.setUserId(event.userId());
        notification.setEventType(event.eventType());
        notification.setRecipientEmail(event.email());
        notification.setMessage(buildMessage(event));
        notification.setStatus(Notification.Status.SENT);

        notificationRepository.save(notification);
        log.info("Notification stored for user {} ({})", event.userId(), event.eventType());
    }

    private String buildMessage(UserEvent event) {
        return switch (event.eventType()) {
            case EventTypes.USER_CREATED -> "Welcome, " + event.fullName() + "! Your account has been created.";
            case EventTypes.USER_UPDATED -> "Hi " + event.fullName() + ", your profile was updated successfully.";
            default -> "Account event received for " + event.email();
        };
    }
}
