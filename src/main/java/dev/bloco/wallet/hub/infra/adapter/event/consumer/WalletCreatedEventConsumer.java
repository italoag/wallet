package dev.bloco.wallet.hub.infra.adapter.event.consumer;

import java.util.function.Consumer;

import org.springframework.context.annotation.Bean;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.context.annotation.Configuration;

import dev.bloco.wallet.hub.domain.event.wallet.WalletCreatedEvent;
import dev.bloco.wallet.hub.infra.adapter.tracing.propagation.CloudEventTracePropagator;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import io.cloudevents.CloudEvent;
import io.micrometer.tracing.Span;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * WalletCreatedEventConsumer processes WalletCreatedEvent messages with distributed tracing.
 *
 * <p>This consumer:</p>
 * <ul>
 *   <li>Receives CloudEvents from Kafka with embedded W3C Trace Context</li>
 *   <li>Extracts trace context and creates CONSUMER spans</li>
 *   <li>Processes wallet creation events</li>
 *   <li>Updates saga state machine</li>
 *   <li>Properly finishes tracing spans</li>
 * </ul>
 *
 * <p>The consumer validates correlation IDs and triggers state transitions:</p>
 * <ul>
 *   <li>Valid correlation ID → WALLET_CREATED state</li>
 *   <li>Missing correlation ID → SAGA_FAILED state</li>
 * </ul>
 */
@Configuration
@Slf4j
public class WalletCreatedEventConsumer {

    @Bean
    public Consumer<Message<CloudEvent>> walletCreatedEventConsumerFunction(StateMachine<SagaStates, SagaEvents> stateMachine,
                                                                             CloudEventTracePropagator tracePropagator) {
        return message -> {
            CloudEvent cloudEvent = message.getPayload();
            Span span = null;

            try {
                // Extract trace context and create CONSUMER span
                span = tracePropagator.extractTraceContext(cloudEvent);
                span.name("consume:WalletCreatedEvent");
                span.tag("event.type", "WalletCreatedEvent");

                // Parse WalletCreatedEvent from CloudEvent data
                String payload = new String(cloudEvent.getData().toBytes());
                log.debug("Processing WalletCreatedEvent payload: {}", payload);

                // Extract correlation ID from JSON payload
                // Simple JSON parsing - in production use Jackson ObjectMapper
                String correlationId = null;
                if (payload.contains("correlationId")) {
                    int start = payload.indexOf("\"correlationId\": \"") + 18;
                    int end = payload.indexOf("\"", start);
                    if (start > 18 && end > start) {
                        correlationId = payload.substring(start, end);
                    }
                }

                span.event("event.parsed");

                // Process event and update state machine
                if (correlationId != null && !correlationId.equals("null")) {
                    var stateMachineMessage = MessageBuilder.withPayload(SagaEvents.WALLET_CREATED)
                            .setHeader("correlationId", java.util.UUID.fromString(correlationId))
                            .build();
                    stateMachine.sendEvent(Mono.just(stateMachineMessage)).subscribe();
                    log.info("Wallet created with correlationId: {}", correlationId);
                } else {
                    stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build())).subscribe();
                    log.warn("Failed to create wallet: Missing correlationId");
                }

                span.end();
                log.info("Successfully processed WalletCreatedEvent with trace context");

            } catch (Exception e) {
                if (span != null) {
                    span.error(e);
                    span.end();
                }
                log.error("Error processing WalletCreatedEvent: {}", e.getMessage(), e);
                throw e;
            }
        };
    }

    /**
     * Legacy consumer function for Message<WalletCreatedEvent> (non-CloudEvent).
     * This maintains backward compatibility until all producers send CloudEvents.
     *
     * @deprecated Use {@link #walletCreatedEventConsumerFunction(StateMachine, CloudEventTracePropagator)} with CloudEvent once producers are updated
     */
    @Deprecated(since = "1.1.0", forRemoval = true)
    public void processLegacyWalletCreatedEvent(Message<WalletCreatedEvent> message, StateMachine<SagaStates, SagaEvents> stateMachine) {
        var event = message.getPayload();
        if (event.getCorrelationId() != null) {
            var stateMachineMessage = MessageBuilder.withPayload(SagaEvents.WALLET_CREATED)
                    .setHeader("correlationId", event.getCorrelationId())
                    .build();
            var result = stateMachine.sendEvent(Mono.just(stateMachineMessage));
            result.subscribe(); // Process the result if needed
            log.info("Wallet created: {}", event.getWalletId());
        } else {
            stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(SagaEvents.SAGA_FAILED).build())).subscribe();
            log.info("Failed to create wallet: Missing correlationId");
        }
    }
}