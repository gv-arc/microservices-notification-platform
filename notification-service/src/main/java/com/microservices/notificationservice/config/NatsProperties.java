package com.microservices.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nats")
public record NatsProperties(
        String url,
        String username,
        String password,
        String streamName,
        String subject,
        String consumerName,
        String durableName
) {
}
