# ObservationRegistry Module

> **Note**: This is a placeholder documentation file. For complete documentation, refer to the actual ObservationRegistry implementation within the TracingConfiguration module.

## Overview

The `ObservationRegistry` is the central coordination point for distributed tracing observations in the WalletHub application. It manages the lifecycle of observations and coordinates tracing handlers for different instrumentation sources.

## Key Responsibilities

1. **Observation Management**: Coordinates the creation and processing of observations
2. **Handler Registration**: Manages tracing observation handlers in proper order
3. **Context Propagation**: Ensures trace context flows correctly through the system
4. **Thread Safety**: Provides thread-safe observation processing for concurrent operations

## Handler Registration Order

The registry registers handlers in a specific order for correct context flow:

1. **PropagatingReceiverTracingObservationHandler**: Extracts trace context from incoming requests/messages
2. **DefaultTracingObservationHandler**: Creates spans for local operations using extracted context
3. **PropagatingSenderTracingObservationHandler**: Injects trace context into outgoing requests/messages

## Integration with TracingHealthIndicator

The `ObservationRegistry` is foundational to the tracing system that `TracingHealthIndicator` monitors:

- **Span Creation**: The registry's handlers create spans that the health indicator tests
- **Tracer Integration**: Works with the `Tracer` instance monitored by the health indicator
- **System Health**: Registry functionality affects overall tracing system health

## Performance Characteristics

- **Overhead**: Typically <1ms per observation
- **Thread Safety**: Designed for concurrent access
- **Context Storage**: Uses ThreadLocal for blocking operations, Reactor Context for reactive operations
- **Sampling**: Applied at Tracer level after observation creation

## Related Modules

- [TracingHealthIndicator](TracingHealthIndicator.md): Monitors health of the tracing system including registry functionality
- [TracingConfiguration](TracingConfiguration.md): Creates and configures the ObservationRegistry
- [TracingFeatureFlags](TracingFeatureFlags.md): Controls which observation sources are enabled