package com.microservices.userservice.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.common.event.UserEvent;
import com.microservices.userservice.domain.OutboxEvent;
import com.microservices.userservice.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);
    private static final int MAX_ATTEMPTS = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final UserEventPublisher userEventPublisher;
    private final ObjectMapper objectMapper;

    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            UserEventPublisher userEventPublisher,
            ObjectMapper objectMapper
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.userEventPublisher = userEventPublisher;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(UserEvent event) {
        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(event.eventId());
        outbox.setEventType(event.eventType());
        try {
            outbox.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize user event", ex);
        }
        outbox.setStatus(OutboxEvent.Status.PENDING);
        outbox.setAttempts(0);
        outboxEventRepository.save(outbox);
    }

    @Scheduled(fixedDelayString = "${outbox.relay-interval-ms:3000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING);
        for (OutboxEvent outbox : pending) {
            relayOne(outbox);
        }
    }

    private void relayOne(OutboxEvent outbox) {
        outbox.setAttempts(outbox.getAttempts() + 1);
        outbox.setLastAttemptAt(Instant.now());
        try {
            UserEvent event = objectMapper.readValue(outbox.getPayload(), UserEvent.class);
            userEventPublisher.publish(event);
            outbox.setStatus(OutboxEvent.Status.SENT);
            outboxEventRepository.save(outbox);
        } catch (Exception ex) {
            log.warn("Outbox relay failed for event {} (attempt {})", outbox.getEventId(), outbox.getAttempts(), ex);
            if (outbox.getAttempts() >= MAX_ATTEMPTS) {
                outbox.setStatus(OutboxEvent.Status.FAILED);
            }
            outboxEventRepository.save(outbox);
        }
    }
}
