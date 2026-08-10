package com.microservices.notificationservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microservices.common.event.UserEvent;
import com.microservices.common.messaging.NatsSubjects;
import com.microservices.notificationservice.config.NatsProperties;
import com.microservices.notificationservice.service.NotificationProcessor;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamSubscription;
import io.nats.client.Message;
import io.nats.client.PushSubscribeOptions;
import io.nats.client.api.AckPolicy;
import io.nats.client.api.ConsumerConfiguration;
import io.nats.client.api.DeliverPolicy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final Connection connection;
    private final ObjectMapper objectMapper;
    private final NatsProperties natsProperties;
    private final NotificationProcessor notificationProcessor;

    private JetStreamSubscription subscription;

    public UserEventListener(
            Connection connection,
            ObjectMapper objectMapper,
            NatsProperties natsProperties,
            NotificationProcessor notificationProcessor
    ) {
        this.connection = connection;
        this.objectMapper = objectMapper;
        this.natsProperties = natsProperties;
        this.notificationProcessor = notificationProcessor;
    }

    @PostConstruct
    public void start() throws IOException, InterruptedException {
        String streamName = natsProperties.streamName() != null ? natsProperties.streamName() : NatsSubjects.STREAM_NAME;
        String subject = (natsProperties.subject() != null ? natsProperties.subject() : NatsSubjects.USER_EVENTS) + ".>";
        String durable = natsProperties.durableName() != null ? natsProperties.durableName() : NatsSubjects.CONSUMER_NAME;

        ConsumerConfiguration consumerConfig = ConsumerConfiguration.builder()
                .durable(durable)
                .ackPolicy(AckPolicy.Explicit)
                .deliverPolicy(DeliverPolicy.All)
                .maxDeliver(5)
                .ackWait(Duration.ofSeconds(30))
                .filterSubject(subject)
                .build();

        PushSubscribeOptions options = PushSubscribeOptions.builder()
                .configuration(consumerConfig)
                .build();

        JetStream jetStream = connection.jetStream();
        subscription = jetStream.subscribe(subject, options);
        subscription.setMessageHandler(this::handleMessage);
        log.info("Subscribed to NATS JetStream subject '{}' with durable consumer '{}'", subject, durable);
    }

    private void handleMessage(Message message) {
        try {
            UserEvent event = objectMapper.readValue(message.getData(), UserEvent.class);
            notificationProcessor.process(event);
            message.ack();
        } catch (Exception ex) {
            log.error("Failed to process message on subject {}: {}",
                    message.getSubject(),
                    new String(message.getData(), StandardCharsets.UTF_8),
                    ex);
            message.nak();
        }
    }

    @PreDestroy
    public void stop() {
        if (subscription != null) {
            try {
                subscription.unsubscribe();
            } catch (IOException ex) {
                log.warn("Error unsubscribing from NATS", ex);
            }
        }
    }
}
