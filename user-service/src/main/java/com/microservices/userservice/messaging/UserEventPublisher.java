package com.microservices.userservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.common.event.UserEvent;
import com.microservices.userservice.config.NatsProperties;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.NatsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Service
public class UserEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisher.class);

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final NatsProperties natsProperties;

    public UserEventPublisher(Connection connection, ObjectMapper objectMapper, NatsProperties natsProperties) {
        this.connection = connection;
        this.objectMapper = objectMapper;
        this.natsProperties = natsProperties;
    }

    public void publish(UserEvent event) {
        String subject = buildSubject(event.eventType());
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            JetStream jetStream = connection.jetStream();
            PublishAck ack = jetStream.publish(
                    NatsMessage.builder()
                            .subject(subject)
                            .data(payload)
                            .headers(new io.nats.client.impl.Headers()
                                    .add("Nats-Msg-Id", event.eventId().toString()))
                            .build()
            );
            log.info("Published {} for user {} (seq={})", event.eventType(), event.userId(), ack.getSeqno());
        } catch (IOException | JetStreamApiException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new EventPublishException("Failed to publish user event to NATS", ex);
        }
    }

    private String buildSubject(String eventType) {
        String base = natsProperties.subject() != null ? natsProperties.subject() : "events.user";
        return base + "." + eventType.toLowerCase();
    }

    public static class EventPublishException extends RuntimeException {
        public EventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
