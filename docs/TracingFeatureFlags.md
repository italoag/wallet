# TracingFeatureFlags Module

> **Note**: This is a placeholder documentation file. For complete documentation, refer to the actual TracingFeatureFlags module implementation.

## Overview

The `TracingFeatureFlags` module provides runtime configuration management for tracing components in the WalletHub application. It enables selective enabling/disabling of tracing features for performance optimization and troubleshooting.

## Feature Flags

The module manages six tracing feature flags:

| Feature Flag | Description | Default | Performance Impact |
|--------------|-------------|---------|-------------------|
| `database` | Database operation tracing (JPA, R2DBC) | `true` | ~1-2ms per query |
| `kafka` | Kafka producer/consumer tracing | `true` | ~0.5-1ms per message |
| `stateMachine` | State machine transition tracing | `true` | ~0.5ms per transition |
| `externalApi` | External API call tracing | `true` | <1ms per request |
| `reactive` | Reactive pipeline tracing | `true` | <0.5ms per operator |
| `useCase` | Use case execution tracing | `true` | ~1-2ms per use case |

## Integration with TracingHealthIndicator

The `TracingHealthIndicator` module depends on `TracingFeatureFlags` to:

1. **Report Feature States**: Include feature flag status in health checks
2. **Monitor Configuration**: Track which tracing components are enabled
3. **Provide Diagnostics**: Help troubleshoot tracing issues by showing enabled/disabled features

## Configuration

Feature flags can be configured via:

```yaml
# application.yml
tracing:
  features:
    database: true
    kafka: true
    stateMachine: true
    externalApi: true
    reactive: true
    useCase: true
```

## Dynamic Updates

Feature flags support dynamic updates via:
- **Configuration Refresh**: `POST /actuator/refresh`
- **Environment Variables**: `TRACING_FEATURES_DATABASE=false`
- **Configuration Management**: External configuration servers

## Related Modules

- [TracingHealthIndicator](TracingHealthIndicator.md): Monitors feature flag states in health checks
- [TracingConfiguration](TracingConfiguration.md): Initializes feature flag configuration
- [TracingMetricsCollector](TracingMetricsCollector.md): Tracks feature flag changes as metrics