# KafkaConsumerObservationHandler - Quick Reference

## Executive Summary

The **KafkaConsumerObservationHandler** is a Spring Boot component that provides distributed tracing for Kafka message consumption in the Wallet Hub application. It creates detailed observability spans for every Kafka consumer operation, enabling end-to-end trace visibility across event-driven workflows.

## Key Benefits

### 🎯 **Observability**
- **Consumer Lag Tracking**: Measures time between message production and consumption
- **Processing Metrics**: Captures deserialization and processing durations
- **Error Diagnostics**: Records detailed error context for troubleshooting
- **Trace Continuity**: Maintains distributed trace context across service boundaries

### ⚡ **Performance**
- **Low Overhead**: <1ms per message impact
- **Feature Flag Control**: Can be disabled via `tracing.features.kafka`
- **Efficient Attributes**: Uses low-cardinality attributes for optimal performance
- **Async Export**: Spans exported asynchronously to tracing backends

### 🔧 **Integration**
- **Spring Cloud Stream**: Native integration with Spring's messaging abstraction
- **OpenTelemetry Compliance**: Follows semantic conventions for messaging systems
- **CloudEvents Support**: Works with CloudEvent trace context propagation
- **Multi-Backend Export**: Supports Tempo (OTLP) and Zipkin backends

## Quick Start Guide

### 1. Enable Tracing
```yaml
# application.yml
tracing:
  features:
    kafka: true  # Enable Kafka consumer tracing
    
management:
  tracing:
    enabled: true
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### 2. Verify Configuration
Check application logs for:
```
Started CONSUMER span for Kafka receive [topic=wallet-events, partition=0, offset=42, group=wallet-service]
```

### 3. Monitor Spans
Access tracing backends:
- **Tempo**: http://localhost:3000 (if using Grafana)
- **Zipkin**: http://localhost:9411

## Common Use Cases

### 🔍 **Troubleshooting Slow Consumers**
```sql
-- In tracing backend, query for:
-- messaging.consumer_lag_ms > 1000
-- messaging.processing_time_ms > 500
```

### 🚨 **Error Investigation**
```sql
-- Find failed consumer spans:
-- status = "error"
-- error.type = "ValidationException"
```

### 📊 **Performance Analysis**
```sql
-- Analyze consumer performance:
-- P99(messaging.processing_time_ms)
-- AVG(messaging.consumer_lag_ms)
-- COUNT(*) by messaging.destination.name
```

## Configuration Reference

### Essential Properties
| Property | Default | Description |
|----------|---------|-------------|
| `tracing.features.kafka` | `true` | Enable/disable Kafka tracing |
| `management.tracing.sampling.probability` | `0.1` | Sample rate (10%) |
| `management.zipkin.tracing.endpoint` | - | Zipkin endpoint URL |
| `management.otlp.tracing.endpoint` | - | OTLP/Tempo endpoint URL |

### Advanced Properties
| Property | Default | Description |
|----------|---------|-------------|
| `tracing.backends.primary` | `tempo` | Primary tracing backend |
| `tracing.backends.fallback` | `zipkin` | Fallback tracing backend |
| `tracing.resilience.circuit-breaker.enabled` | `true` | Enable circuit breaker |

## Span Attributes Reference

### Core Messaging Attributes
| Attribute | Example | Description |
|-----------|---------|-------------|
| `messaging.system` | `"kafka"` | Message broker system |
| `messaging.operation` | `"receive"` | Consumer operation type |
| `messaging.destination.name` | `"wallet-events"` | Kafka topic name |
| `messaging.kafka.partition` | `"0"` | Partition number |
| `messaging.kafka.offset` | `"42"` | Message offset |
| `messaging.kafka.consumer.group` | `"wallet-service"` | Consumer group ID |

### Performance Attributes
| Attribute | Example | Description |
|-----------|---------|-------------|
| `messaging.consumer_lag_ms` | `"125"` | Producer → consumer delay |
| `messaging.processing_time_ms` | `"45"` | Total processing time |
| `messaging.kafka.deserialization_time_ms` | `"5"` | Deserialization duration |

### Status Attributes
| Attribute | Example | Description |
|-----------|---------|-------------|
| `status` | `"success"` | Processing outcome |
| `error.type` | `"ValidationException"` | Error class name |
| `span.kind` | `"CONSUMER"` | Span type |

## Troubleshooting Checklist

### ❌ No Consumer Spans Appearing
- [ ] Verify `tracing.features.kafka=true`
- [ ] Check Spring Cloud Stream configuration
- [ ] Verify ObservationRegistry bean creation
- [ ] Check tracing backend connectivity

### ❌ Missing Trace Context
- [ ] Ensure producers use `CloudEventTracePropagator`
- [ ] Verify CloudEvent has `traceparent` extension
- [ ] Check producer tracing configuration

### ❌ High Processing Latency
- [ ] Review `messaging.processing_time_ms`
- [ ] Check consumer business logic
- [ ] Monitor system resources
- [ ] Consider increasing consumer instances

### ❌ Consumer Lag Increasing
- [ ] Monitor `messaging.consumer_lag_ms`
- [ ] Check producer rate vs consumer rate
- [ ] Review partition assignment
- [ ] Consider consumer scaling

## Integration Examples

### Basic Consumer with Tracing
```java
@Component
public class WalletEventConsumer {
    
    @Bean
    public Consumer<Message<CloudEvent>> consumeWalletEvents(
            CloudEventTracePropagator tracePropagator) {
        
        return message -> {
            CloudEvent event = message.getPayload();
            Span span = tracePropagator.extractTraceContext(event);
            
            try {
                // Process event
                processWalletEvent(event);
            } catch (Exception e) {
                span.error(e);
                throw e;
            } finally {
                span.end();
            }
        };
    }
}
```

### Custom Attribute Injection
```java
// The handler automatically adds standard attributes
// For custom business attributes, use SpanAttributeBuilder:

@Autowired
private SpanAttributeBuilder spanAttributeBuilder;

// In consumer logic:
spanAttributeBuilder.addWalletOperationAttributes(
    span, walletId, "funds_added", transactionId, amount, currency);
```

## Performance Optimization Tips

### For High-Volume Systems
1. **Enable Sampling**: Set `management.tracing.sampling.probability=0.01` (1%)
2. **Disable Non-Critical**: Turn off tracing for low-value topics
3. **Monitor Memory**: Watch for span accumulation during backpressure
4. **Use Async Processing**: Ensure span export doesn't block consumer threads

### For Latency-Sensitive Applications
1. **Disable Tracing**: Set `tracing.features.kafka=false` for critical paths
2. **Minimal Attributes**: Use only essential span attributes
3. **Batch Processing**: Aggregate spans for batch consumers
4. **Local Testing**: Profile overhead in test environment

## Related Components

| Component | Purpose | Relationship |
|-----------|---------|--------------|
| **KafkaProducerObservationHandler** | Producer-side tracing | Complementary component |
| **CloudEventTracePropagator** | Trace context propagation | Dependency for trace continuity |
| **SpanAttributeBuilder** | Attribute management | Used for consistent attribute naming |
| **TracingConfiguration** | Overall tracing setup | Registers handler in ObservationRegistry |
| **TracingFeatureFlags** | Feature control | Enables/disables Kafka tracing |

## Support and Resources

### Documentation
- [Full Documentation](KafkaConsumerObservationHandler.md): Comprehensive module documentation
- [Tracing Architecture](TracingConfiguration.md): Overall tracing infrastructure
- [CloudEvents Integration](CloudEventTracePropagator.md): Trace context propagation details

### Monitoring Endpoints
- **Health Check**: `GET /actuator/health/tracing`
- **Configuration**: `GET /actuator/configprops/tracing.features`
- **Metrics**: `GET /actuator/metrics/observation`

### Alerting Rules
```yaml
# Example Prometheus alert rules
- alert: HighConsumerLag
  expr: messaging_consumer_lag_ms > 1000
  for: 5m
  
- alert: ConsumerErrorRateHigh
  expr: rate(messaging_errors_total[5m]) > 0.1
```

## Version Compatibility

| Wallet Hub Version | Spring Boot | Spring Cloud | Kafka Client |
|-------------------|-------------|--------------|--------------|
| 1.0.0 | 3.2.x | 2023.0.x | 3.6.x |
| 1.1.0 | 3.3.x | 2024.0.x | 3.7.x |
| 1.2.0 | 3.4.x | 2024.1.x | 3.8.x |

## Migration Notes

### From Version 1.0 to 1.1
- Added CloudEvents trace context support
- Improved consumer lag calculation
- Added feature flag configuration
- Enhanced error handling

### Planned for Version 1.2
- Dynamic sampling based on message rate
- Batch consumer support
- Custom attribute injection API
- Enhanced metrics integration

---

**Next Steps**: For detailed implementation, configuration, and troubleshooting, refer to the [full documentation](KafkaConsumerObservationHandler.md).