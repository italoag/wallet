package dev.bloco.wallet.hub.infra.util;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * Utility class for creating CloudEvent instances with various configurations.
 */
public class CloudEventUtils {

  /**
   * Creates a CloudEvent with the specified data, type, and source URI.
   * This method generates a unique ID for the event and includes the provided payload data.
   *
   * @param <T>   The type of the data payload.
   * @param data  The payload data to include in the CloudEvent. The data is serialized to JSON bytes.
   * @param type  The type of the CloudEvent, identifying the nature of the event.
   * @param source The URI that identifies the source of the CloudEvent.
   * @return A CloudEvent instance containing the specified attributes and data payload.
   */
    public static <T> CloudEvent createCloudEvent(T data, String type, String source) {
        byte[] json = toJsonBytes(data);
        return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType(type)
                .withSource(URI.create(source))
                .withDataContentType("application/json")
                .withData(json)
                .build();
    }

  /**
   * Creates a CloudEvent with the specified data, type, source URI, and correlation ID.
   * This method generates a unique ID for the event, attaches the provided payload data,
   * and includes the correlation ID as an extension attribute.
   *
   * @param <T>           The type of the data payload.
   * @param data          The payload data to include in the CloudEvent. The data is serialized to JSON bytes.
   * @param type          The type of the CloudEvent, identifying the nature of the event.
   * @param source        The URI that identifies the source of the CloudEvent.
   * @param correlationId The ID used to correlate the event with related events or actions.
   * @return A CloudEvent instance containing the specified attributes, data payload, and the correlation ID.
   */
    public static <T> CloudEvent createCloudEvent(T data, String type, String source, String correlationId) {
        byte[] json = toJsonBytes(data);
        return CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withType(type)
                .withSource(URI.create(source))
                .withExtension("correlationid", correlationId)
                .withDataContentType("application/json")
                .withData(json)
                .build();
    }

    private static byte[] toJsonBytes(Object data) {
        try {
            // Use Jackson 3 with builder pattern - modules are auto-discovered via SPI
            tools.jackson.databind.json.JsonMapper om = tools.jackson.databind.json.JsonMapper.builder()
                    .findAndAddModules()
                    .build();
            return om.writeValueAsBytes(data);
        } catch (Exception ex) {
            // Fallback to toString() if serialization fails to avoid hard failures
            return String.valueOf(data).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
