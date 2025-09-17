package dev.bloco.wallet.hub.infra.util;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

import java.net.URI;
import java.util.UUID;

public class CloudEventUtils {

    public static <T> CloudEvent createCloudEvent(T data, String type, String source) {
        return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType(type)
                .withSource(URI.create(source))
                .withData(data.toString().getBytes())
                .build();
    }

    public static <T> CloudEvent createCloudEvent(T data, String type, String source, String correlationId) {
        return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType(type)
                .withSource(URI.create(source))
                .withExtension("correlationid", correlationId)
                .withData(data.toString().getBytes())
                .build();
    }
}
