package com.microservices.notificationservice.config;

import com.microservices.common.messaging.NatsSubjects;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
public class NatsConfig {

    private static final Logger log = LoggerFactory.getLogger(NatsConfig.class);

    @Bean(destroyMethod = "close")
    public Connection natsConnection(NatsProperties properties) throws IOException, InterruptedException {
        Options.Builder builder = new Options.Builder()
                .server(properties.url())
                .connectionTimeout(Duration.ofSeconds(5))
                .reconnectWait(Duration.ofSeconds(2))
                .maxReconnects(-1)
                .pingInterval(Duration.ofSeconds(20));

        if (properties.username() != null && !properties.username().isBlank()) {
            builder.userInfo(properties.username().toCharArray(), properties.password().toCharArray());
        }

        Connection connection = Nats.connect(builder.build());
        log.info("Connected to NATS at {}", properties.url());
        return connection;
    }
}
