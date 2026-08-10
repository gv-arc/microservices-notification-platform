package com.microservices.notificationservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({NatsProperties.class, JwtProperties.class})
public class ApplicationConfig {
}
