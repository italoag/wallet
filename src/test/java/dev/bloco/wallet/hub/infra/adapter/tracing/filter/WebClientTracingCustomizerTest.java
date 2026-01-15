package dev.bloco.wallet.hub.infra.adapter.tracing.filter;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import dev.bloco.wallet.hub.infra.adapter.tracing.config.TracingFeatureFlags;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link WebClientTracingCustomizer}.
 * 
 * <h2>Test Strategy</h2>
 * <ul>
 *   <li>Mock Tracer, Propagator, and SensitiveDataSanitizer</li>
 *   <li>Create WebClient with customizer applied</li>
 *   <li>Mock ExchangeFunction to return controlled responses</li>
 *   <li>Verify span creation, attributes, and lifecycle via ArgumentCaptor</li>
 *   <li>Test W3C Trace Context injection via Propagator.inject()</li>
 *   <li>Validate error handling and timeout scenarios</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WebClientTracingCustomizer Unit Tests")
class WebClientTracingCustomizerTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Propagator propagator;

    @Mock
    private SensitiveDataSanitizer sanitizer;

    @Mock
    private TracingFeatureFlags featureFlags;

    @Mock
    private Span span;

    @Mock
    private TraceContext traceContext;

    @Mock
    private ExchangeFunction exchangeFunction;

    private WebClientTracingCustomizer customizer;

    @BeforeEach
    void setUp() {
        customizer = new WebClientTracingCustomizer(tracer, propagator, sanitizer, featureFlags);

        // Default mock behavior (lenient to avoid unnecessary stubbing errors)
        lenient().when(featureFlags.isExternalApi()).thenReturn(true);
        lenient().when(tracer.nextSpan()).thenReturn(span);
        lenient().when(span.name(anyString())).thenReturn(span);
        lenient().when(span.start()).thenReturn(span);
        lenient().when(span.context()).thenReturn(traceContext);
        lenient().when(traceContext.traceId()).thenReturn("test-trace-id");
        lenient().when(sanitizer.sanitizeUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Should create CLIENT span for HTTP GET request")
    void shouldCreateClientSpanForHttpGet() {
        // Arrange
        ClientRequest request = ClientRequest.create(HttpMethod.GET, URI.create("https://api.example.com/users"))
            .build();

        ClientResponse response = ClientResponse.create(HttpStatus.OK)
            .header(HttpHeaders.CONTENT_LENGTH, "1024")
            .build();

        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.get()
            .uri("https://api.example.com/users")
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty()) // Handle error for test
            .block();

        // Assert
        verify(tracer, atLeastOnce()).nextSpan();
        verify(span, atLeastOnce()).name("HTTP GET");
        verify(span, atLeastOnce()).start();
        verify(span, atLeastOnce()).tag(eq("http.method"), eq("GET"));
        verify(span, atLeastOnce()).tag(eq("span.kind"), eq("CLIENT"));
        verify(span, atLeastOnce()).end();
    }

    @Test
    @DisplayName("Should add HTTP status code and response size attributes")
    void shouldAddHttpResponseAttributes() {
        // Arrange
        ClientRequest request = ClientRequest.create(HttpMethod.POST, URI.create("https://api.example.com/data"))
            .header(HttpHeaders.CONTENT_LENGTH, "512")
            .build();

        ClientResponse response = ClientResponse.create(HttpStatus.CREATED)
            .header(HttpHeaders.CONTENT_LENGTH, "2048")
            .build();

        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.post()
            .uri("https://api.example.com/data")
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty())
            .block();

        // Assert
        verify(span, atLeastOnce()).tag(eq("http.status_code"), eq("201"));
        verify(span, atLeastOnce()).tag(eq("http.response.body.size"), eq("2048"));
        verify(span, atLeastOnce()).tag(eq("status"), eq("success"));
    }

    @Test
    @DisplayName("Should inject traceparent header via W3C Trace Context propagator")
    void shouldInjectTraceparentHeader() {
        // Arrange
        ClientResponse response = ClientResponse.create(HttpStatus.OK).build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.get()
            .uri("https://api.example.com/trace")
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty())
            .block();

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Propagator.Setter<Object>> setterCaptor = ArgumentCaptor.forClass(Propagator.Setter.class);
        verify(propagator, atLeastOnce()).inject(eq(traceContext), any(), setterCaptor.capture());
    }

    @Test
    @DisplayName("Should mark span as error for 4xx/5xx HTTP status codes")
    void shouldMarkSpanAsErrorForClientAndServerErrors() {
        // Arrange
        ClientResponse response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR).build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.get()
            .uri("https://api.example.com/error")
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty())
            .block();

        // Assert
        verify(span, atLeastOnce()).tag(eq("http.status_code"), eq("500"));
        verify(span, atLeastOnce()).tag(eq("status"), eq("error"));
        verify(span, atLeastOnce()).tag(eq("error.type"), eq("HTTP_500"));
    }

    @Test
    @DisplayName("Should add error attributes on exception")
    void shouldAddErrorAttributesOnException() {
        // Arrange
        RuntimeException error = new RuntimeException("Connection timeout");
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.error(error));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.get()
            .uri("https://api.example.com/timeout")
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty())
            .block();

        // Assert
        verify(span, atLeastOnce()).tag(eq("error.type"), eq("RuntimeException"));
        verify(span, atLeastOnce()).tag(eq("error.message"), eq("Connection timeout"));
        verify(span, atLeastOnce()).tag(eq("status"), eq("error"));
        verify(span, atLeastOnce()).end();
    }

    @Test
    @DisplayName("Should calculate and add request duration")
    void shouldCalculateRequestDuration() throws InterruptedException {
        // Arrange
        ClientResponse response = ClientResponse.create(HttpStatus.OK).build();
        when(exchangeFunction.exchange(any(ClientRequest.class)))
            .thenAnswer(invocation -> {
                Thread.sleep(10); // Simulate 10ms delay
                return Mono.just(response);
            });

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.get()
            .uri("https://api.example.com/slow")
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty())
            .block();

        // Assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<String> durationCaptor = ArgumentCaptor.forClass(String.class);
        verify(span, atLeastOnce()).tag(eq("http.duration_ms"), durationCaptor.capture());

        // Duration should be >= 10ms
        assertThat(Integer.parseInt(durationCaptor.getValue())).isGreaterThanOrEqualTo(10);
    }

    @Test
    @DisplayName("Should sanitize URL before adding to span")
    void shouldSanitizeUrlBeforeAddingToSpan() {
        // Arrange
        String originalUrl = "https://api.example.com/users?token=secret123&apiKey=key456";
        String sanitizedUrl = "https://api.example.com/users?token=***&apiKey=***";
        when(sanitizer.sanitizeUrl(originalUrl)).thenReturn(sanitizedUrl);

        ClientResponse response = ClientResponse.create(HttpStatus.OK).build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.get()
            .uri(originalUrl)
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty())
            .block();

        // Assert
        verify(sanitizer, atLeastOnce()).sanitizeUrl(originalUrl);
        verify(span, atLeastOnce()).tag(eq("http.url"), eq(sanitizedUrl));
    }

    @Test
    @DisplayName("Should not customize when feature flag is disabled")
    void shouldNotCustomizeWhenFeatureFlagDisabled() {
        // Arrange
        when(featureFlags.isExternalApi()).thenReturn(false);

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);

        // Act
        customizer.customize(builder);

        // Assert
        verifyNoInteractions(tracer);
        verifyNoInteractions(propagator);
    }

    @Test
    @DisplayName("Should skip tracing when feature flag disabled at filter level")
    void shouldSkipTracingWhenFeatureFlagDisabledAtFilterLevel() {
        // Arrange
        when(featureFlags.isExternalApi()).thenReturn(true).thenReturn(false); // Enable at customize, disable at filter

        ClientResponse response = ClientResponse.create(HttpStatus.OK).build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.get()
            .uri("https://api.example.com/no-trace")
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty())
            .block();

        // Assert - tracer should not be called due to disabled flag
        verify(tracer, never()).nextSpan();
    }

    @Test
    @DisplayName("Should handle null response gracefully")
    void shouldHandleNullResponseGracefully() {
        // Arrange
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.empty());

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.get()
            .uri("https://api.example.com/empty")
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty())
            .block();

        // Assert - span should still be created and ended
        verify(span, atLeastOnce()).start();
        verify(span, atLeastOnce()).end();
    }

    @Test
    @DisplayName("Should add request body size when available")
    void shouldAddRequestBodySizeWhenAvailable() {
        // Arrange
        ClientResponse response = ClientResponse.create(HttpStatus.OK).build();
        when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(response));

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        customizer.customize(builder);
        WebClient webClient = builder.build();

        // Act
        webClient.post()
            .uri("https://api.example.com/data")
            .header(HttpHeaders.CONTENT_LENGTH, "1024")
            .retrieve()
            .bodyToMono(String.class)
            .onErrorResume(e -> Mono.empty())
            .block();

        // Assert
        verify(span, atLeastOnce()).tag(eq("http.request.body.size"), eq("1024"));
    }
}
