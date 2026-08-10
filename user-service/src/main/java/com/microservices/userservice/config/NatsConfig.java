package com.microservices.userservice.config;

import com.microservices.common.messaging.NatsSubjects;
import io.nats.client.Connection;
import io.nats.client.JetStreamManagement;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.RetentionPolicy;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(NatsProperties.class)
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
        ensureStreamExists(connection, properties);
        return connection;
    }

    private void ensureStreamExists(Connection connection, NatsProperties properties) throws IOException, InterruptedException {
        JetStreamManagement management = connection.jetStreamManagement();
        String streamName = properties.streamName() != null ? properties.streamName() : NatsSubjects.STREAM_NAME;
        String subject = properties.subject() != null ? properties.subject() : NatsSubjects.USER_EVENTS;

        try {
            management.getStreamInfo(streamName);
            log.info("NATS JetStream '{}' already exists", streamName);
        } catch (Exception ex) {
            StreamConfiguration config = StreamConfiguration.builder()
                    .name(streamName)
                    .subjects(subject + ".>")
                    .storageType(StorageType.File)
                    .retentionPolicy(RetentionPolicy.Limits)
                    .maxAge(Duration.ofDays(7))
                    .duplicateWindow(Duration.ofMinutes(2))
                    .build();
            management.addStream(config);
            log.info("Created NATS JetStream '{}' for subject '{}'", streamName, subject);
        }
    }
}
