package dev.bloco.wallet.hub.infra.provider.data.repository;

import static dev.bloco.wallet.hub.infra.provider.data.repository.ChainlistNetworkRepository.CORRELATION_HEADER;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bloco.wallet.hub.domain.model.network.Network;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
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
    void tearDown() throws IOException {
        mockWebServer.shutdown();
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

        mockWebServer.enqueue(new MockResponse()
                .setBody(payload)
                .setHeader("Content-Type", "application/json"));

        List<Network> networks = repository.findAll(CORRELATION_ID);

        assertThat(networks).hasSize(1);
        Network network = networks.get(0);
        assertThat(network.getName()).isEqualTo("Sample Chain");
        assertThat(network.getChainId()).isEqualTo("12345");
        assertThat(network.getRpcUrl()).isEqualTo("https://sample.rpc");
        assertThat(network.getExplorerUrl()).isEqualTo("https://explorer.sample");

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getHeader(CORRELATION_HEADER)).isEqualTo(CORRELATION_ID);
        assertThat(recordedRequest.getPath()).isEqualTo("/rpcs.json");
    }
}
