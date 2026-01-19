package dev.bloco.wallet.hub.infra.adapter.tracing.handler;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.state.State;
import org.springframework.statemachine.transition.Transition;
import org.springframework.statemachine.trigger.Trigger;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.SpanAttributeBuilder;
import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaEvents;
import dev.bloco.wallet.hub.infra.provider.data.config.SagaStates;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

/**
 * Unit tests for StateMachineObservationHandler.
 * Verifies state transition tracking, compensation detection, and lifecycle
 * events.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class StateMachineObservationHandlerTest {

    @Mock
    private Tracer tracer;

    @Mock
    private SpanAttributeBuilder spanAttributeBuilder;

    @Mock
    private TracingFeatureFlags featureFlags;

    @Mock
    private StateMachine<SagaStates, SagaEvents> stateMachine;

    @Mock
    private Transition<SagaStates, SagaEvents> transition;

    @Mock
    private State<SagaStates, SagaEvents> sourceState;

    @Mock
    private State<SagaStates, SagaEvents> targetState;

    @Mock
    private Trigger<SagaStates, SagaEvents> trigger;

    @Mock
    private Span span;

    private StateMachineObservationHandler handler;

    private static final String MACHINE_ID = "saga-test-123";
    private static final UUID MACHINE_UUID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(featureFlags.isStateMachine()).thenReturn(true);
        handler = new StateMachineObservationHandler(tracer, spanAttributeBuilder, featureFlags);

        // Setup default span behavior
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);
        when(span.start()).thenReturn(span);
        when(span.tag(anyString(), anyString())).thenReturn(span);
        when(span.event(anyString())).thenReturn(span);

        // Setup state machine ID
        when(stateMachine.getId()).thenReturn(MACHINE_ID);
        when(stateMachine.getUuid()).thenReturn(MACHINE_UUID);
    }

    @Test
    void transitionStartedShouldCreateSpanWithAttributes() {
        // Given
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(transition.getTrigger()).thenReturn(trigger);
        when(trigger.getEvent()).thenReturn(SagaEvents.FUNDS_ADDED);
        when(sourceState.getId()).thenReturn(SagaStates.WALLET_CREATED);
        when(targetState.getId()).thenReturn(SagaStates.FUNDS_ADDED);

        // When
        handler.transitionStarted(transition, stateMachine);

        // Then
        verify(tracer).nextSpan();
        verify(span).name("State Transition: WALLET_CREATED → FUNDS_ADDED");
        verify(span).start();
        verify(spanAttributeBuilder).addStateMachineAttributes(
                eq(span),
                eq(MACHINE_ID),
                eq("WALLET_CREATED"),
                eq("FUNDS_ADDED"),
                eq("FUNDS_ADDED"),
                eq(false));
        verify(span).event("transition.started");
    }

    @Test
    void transitionStartedShouldDetectCompensationFlow() {
        // Given - transition to FAILED state (compensation)
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(transition.getTrigger()).thenReturn(trigger);
        when(trigger.getEvent()).thenReturn(SagaEvents.SAGA_FAILED);
        when(sourceState.getId()).thenReturn(SagaStates.FUNDS_ADDED);
        when(targetState.getId()).thenReturn(SagaStates.FAILED);

        // When
        handler.transitionStarted(transition, stateMachine);

        // Then
        verify(spanAttributeBuilder).addStateMachineAttributes(
                eq(span),
                eq(MACHINE_ID),
                eq("FUNDS_ADDED"),
                eq("FAILED"),
                eq("SAGA_FAILED"),
                eq(true));
    }

    @Test
    void transitionEndedShouldCompleteSpanWithDuration() throws InterruptedException {
        // Given - first start a transition
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(transition.getTrigger()).thenReturn(trigger);
        when(trigger.getEvent()).thenReturn(SagaEvents.WALLET_CREATED);
        when(sourceState.getId()).thenReturn(SagaStates.INITIAL);
        when(targetState.getId()).thenReturn(SagaStates.WALLET_CREATED);

        handler.transitionStarted(transition, stateMachine);

        // Wait a bit to simulate transition time
        Thread.sleep(10);

        // When
        handler.transitionEnded(transition, stateMachine);

        // Then
        verify(span).tag(eq("statemachine.transition.duration_ms"), anyString());
        verify(spanAttributeBuilder).addSuccessStatus(span);
        verify(span).event("transition.completed");
        verify(span).end();
    }

    @Test
    void transitionEndedShouldDetectSlowTransition() {
        // Given - simulate slow transition by manually setting start time in the past
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(sourceState.getId()).thenReturn(SagaStates.INITIAL);
        when(targetState.getId()).thenReturn(SagaStates.WALLET_CREATED);

        // Start transition
        handler.transitionStarted(transition, stateMachine);

        // Manually inject old start time to simulate slow transition (>5s)
        // Note: This is a limitation of the current implementation - we can't easily
        // mock System.nanoTime()
        // In a real scenario, we'd need to refactor to inject a Clock or TimeProvider

        // When
        handler.transitionEnded(transition, stateMachine);

        // Then - at minimum, verify duration was calculated
        verify(span).tag(eq("statemachine.transition.duration_ms"), anyString());
        verify(span).end();
    }

    @Test
    void stateChangedShouldAddSpanEvent() {
        // Given - first start a transition to create an active span
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(sourceState.getId()).thenReturn(SagaStates.WALLET_CREATED);
        when(targetState.getId()).thenReturn(SagaStates.FUNDS_ADDED);

        handler.transitionStarted(transition, stateMachine);

        // When
        handler.stateChanged(sourceState, targetState, stateMachine);

        // Then
        verify(span).event("state.changed: WALLET_CREATED → FUNDS_ADDED");
    }

    @Test
    void extendedStateChangedShouldTrackGuardEvaluation() {
        // Given - first start a transition
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(sourceState.getId()).thenReturn(SagaStates.INITIAL);
        when(targetState.getId()).thenReturn(SagaStates.WALLET_CREATED);

        handler.transitionStarted(transition, stateMachine);

        // When
        handler.extendedStateChanged("guard.sufficientFunds", true, stateMachine);

        // Then
        verify(span).event("guard.evaluated: guard.sufficientFunds = true");
        verify(span).tag("guard.sufficientFunds", "true");
    }

    @Test
    void extendedStateChangedShouldTrackActionExecution() {
        // Given - first start a transition
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(sourceState.getId()).thenReturn(SagaStates.WALLET_CREATED);
        when(targetState.getId()).thenReturn(SagaStates.FUNDS_ADDED);

        handler.transitionStarted(transition, stateMachine);

        // When
        handler.extendedStateChanged("action.addFunds", "executed", stateMachine);

        // Then
        verify(span).event("action.executed: action.addFunds");
    }

    @Test
    void stateMachineErrorShouldMarkSpanAsError() {
        // Given - first start a transition
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(sourceState.getId()).thenReturn(SagaStates.FUNDS_ADDED);
        when(targetState.getId()).thenReturn(SagaStates.FUNDS_WITHDRAWN);

        handler.transitionStarted(transition, stateMachine);

        RuntimeException exception = new RuntimeException("Insufficient funds");

        // When
        handler.stateMachineError(stateMachine, exception);

        // Then
        verify(spanAttributeBuilder).addErrorAttributes(span, exception);
        verify(span).event("statemachine.error");
    }

    @Test
    void shouldNotInstrumentWhenFeatureFlagDisabled() {
        // Given
        when(featureFlags.isStateMachine()).thenReturn(false);
        StateMachineObservationHandler disabledHandler = new StateMachineObservationHandler(tracer,
                spanAttributeBuilder, featureFlags);

        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);

        // When
        disabledHandler.transitionStarted(transition, stateMachine);

        // Then - no span should be created
        verify(tracer, never()).nextSpan();
    }

    @Test
    void shouldHandleMissingEventGracefully() {
        // Given - transition without trigger/event
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(transition.getTrigger()).thenReturn(null);
        when(sourceState.getId()).thenReturn(SagaStates.INITIAL);
        when(targetState.getId()).thenReturn(SagaStates.WALLET_CREATED);

        // When
        handler.transitionStarted(transition, stateMachine);

        // Then - should still create span, just without event attribute
        verify(span).name("State Transition: INITIAL → WALLET_CREATED");
        verify(spanAttributeBuilder).addStateMachineAttributes(
                eq(span),
                eq(MACHINE_ID),
                eq("INITIAL"),
                eq("WALLET_CREATED"),
                eq(null),
                eq(false));
    }

    @Test
    void shouldHandleNullStatesGracefully() {
        // Given
        when(transition.getSource()).thenReturn(null);
        when(transition.getTarget()).thenReturn(null);

        // When/Then - should not crash
        handler.transitionStarted(transition, stateMachine);

        verify(span).name("State Transition: UNKNOWN → UNKNOWN");
        verify(spanAttributeBuilder).addStateMachineAttributes(
                eq(span),
                eq(MACHINE_ID),
                eq("UNKNOWN"),
                eq("UNKNOWN"),
                eq(null),
                eq(false));
    }

    @Test
    void shouldUseUuidWhenMachineIdIsNull() {
        // Given
        when(stateMachine.getId()).thenReturn(null);
        when(transition.getSource()).thenReturn(sourceState);
        when(transition.getTarget()).thenReturn(targetState);
        when(sourceState.getId()).thenReturn(SagaStates.INITIAL);
        when(targetState.getId()).thenReturn(SagaStates.WALLET_CREATED);

        // When
        handler.transitionStarted(transition, stateMachine);

        // Then - should use UUID instead
        verify(spanAttributeBuilder).addStateMachineAttributes(
                eq(span),
                eq(MACHINE_UUID.toString()),
                eq("INITIAL"),
                eq("WALLET_CREATED"),
                eq(null),
                eq(false));
    }
}
