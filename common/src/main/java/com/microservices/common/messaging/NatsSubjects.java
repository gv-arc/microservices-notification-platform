package com.microservices.common.messaging;

public final class NatsSubjects {

    public static final String USER_EVENTS = "events.user";
    public static final String STREAM_NAME = "USER_EVENTS";
    public static final String CONSUMER_NAME = "notification-service-consumer";

    private NatsSubjects() {
    }
}
