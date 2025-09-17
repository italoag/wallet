package dev.bloco.wallet.hub.infra.util;

import io.cloudevents.CloudEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CloudEvents Utils Tests")
class CloudEventUtilsTest {

    @Test
    @DisplayName("Creates a basic CloudEvent with basic attributes")
    void createCloudEvent_withoutCorrelationId_setsBasicAttributes() {
        // given
        var data = "payload-data";
        var type = "demo.type";
        var source = "https://source.example/test";

        // when
        CloudEvent event = CloudEventUtils.createCloudEvent(data, type, source);

        // then
        assertThat(event.getId()).isNotNull();
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getSource()).isEqualTo(URI.create(source));
        assertThat(event.getData()).isNotNull();
        assertThat(new String(event.getData().toBytes())).isEqualTo(data);
        // no correlationId extension present
        assertThat(event.getExtensionNames()).doesNotContain("correlationid");
    }

    @Test
    @DisplayName("Creates a basic CloudEvent with basic attributes and correlationId")
    void createCloudEvent_withCorrelationId_addsExtension() {
        // given
        var data = "another-payload";
        var type = "demo.type.2";
        var source = "urn:test:source";
        var correlationId = "corr-123";

        // when
        CloudEvent event = CloudEventUtils.createCloudEvent(data, type, source, correlationId);

        // then
        assertThat(event.getId()).isNotNull();
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getSource()).isEqualTo(URI.create(source));
        assertThat(new String(event.getData().toBytes())).isEqualTo(data);
        // CloudEvents SDK stores extension keys in lowercase
        assertThat(event.getExtensionNames()).contains("correlationid");
        assertThat(event.getExtension("correlationid")).isEqualTo(correlationId);
    }
}
