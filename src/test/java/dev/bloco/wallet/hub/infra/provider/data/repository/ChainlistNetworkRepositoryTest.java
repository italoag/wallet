package dev.bloco.wallet.hub.infra.provider.data.repository;

import static dev.bloco.wallet.hub.infra.provider.data.repository.ChainlistNetworkRepository.CORRELATION_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.ObjectMapper;
import dev.bloco.wallet.hub.domain.model.network.Network;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@DisplayName("Chainlist Network Repository Tests")
class ChainlistNetworkRepositoryTest {

    private static final String CORRELATION_ID = UUID.randomUUID().toString();

    private MockWebServer mockWebServer;
    private ChainlistNetworkRepository repository;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient.Builder builder = WebClient.builder();
        ObjectMapper objectMapper = new ObjectMapper();
        repository = new ChainlistNetworkRepository(
                builder,
                objectMapper,
                mockWebServer.url("/rpcs.json").toString(),
                Duration.ofMinutes(5)
        );
    }

    @AfterEach
    void tearDown() {
        mockWebServer.close();
    }

    @Test
    @DisplayName("findAll should fetch networks from chainlist")
    void findAll_shouldFetchNetworksFromChainlist() throws InterruptedException {
        String payload = "[" +
                "{" +
                "\"name\":\"Sample Chain\"," +
                "\"chainId\":12345," +
                "\"rpc\":[{\"url\":\"https://sample.rpc\"}]," +
                "\"explorers\":[{\"url\":\"https://explorer.sample\"}]" +
                "}" +
                "]";

        // MockWebServer3 uses builder pattern for MockResponse
        MockResponse response = new MockResponse.Builder()
                .body(payload)
                .addHeader("Content-Type", "application/json")
                .build();
        mockWebServer.enqueue(response);

        List<Network> networks = repository.findAll(CORRELATION_ID);

        assertThat(networks).hasSize(1);
        Network network = networks.get(0);
        assertThat(network.getName()).isEqualTo("Sample Chain");
        assertThat(network.getChainId()).isEqualTo("12345");
        assertThat(network.getRpcUrl()).isEqualTo("https://sample.rpc");
        assertThat(network.getExplorerUrl()).isEqualTo("https://explorer.sample");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        // MockWebServer3: use getHeaders().get() instead of getHeader()
        assertThat(recordedRequest.getHeaders().get(CORRELATION_HEADER)).isEqualTo(CORRELATION_ID);
        // MockWebServer3: use getUrl().encodedPath() instead of getPath()
        assertThat(recordedRequest.getUrl().encodedPath()).isEqualTo("/rpcs.json");
    }
}
