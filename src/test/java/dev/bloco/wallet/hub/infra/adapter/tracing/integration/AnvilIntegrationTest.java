package dev.bloco.wallet.hub.infra.adapter.tracing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
@DisplayName("Anvil Integration Test")
class AnvilIntegrationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String TARGET_ADDRESS = "0xc9019F78FE72655403A52baCA31902E8f8944a8f";
    private static final long POLYGON_AMOY_CHAIN_ID = 80002L;

    static {
        System.setProperty("TESTCONTAINERS_RYUK_DISABLED", "true");
        System.setProperty("TESTCONTAINERS_HOST_OVERRIDE", "127.0.0.1");
    }

    private static final GenericContainer<?> ANVIL_CONTAINER = new GenericContainer<>(
            DockerImageName.parse("ghcr.io/foundry-rs/foundry:latest"))
            .withEnv("FOUNDRY_DISABLE_NIGHTLY_WARNING", "true")
            .withFileSystemBind(System.getProperty("user.home") + "/.certificates/santander-bundle.pem",
                    "/etc/ssl/certs/ca-certificates.crt", org.testcontainers.containers.BindMode.READ_ONLY)
            .withLogConsumer(new org.testcontainers.containers.output.Slf4jLogConsumer(log).withSeparateOutputStreams())
            .withCreateContainerCmdModifier(cmd -> {
                cmd.withEntrypoint("anvil",
                    "--host", "0.0.0.0",
                    "--port", "8545",
                    "--fork-url", "https://rpc-amoy.polygon.technology",
                    "--chain-id", "80002",
                    "--accounts", "10",
                    "--balance", "1000");
            })
            .withExposedPorts(8545)
            .waitingFor(Wait.forLogMessage(".*Listening on.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(30)));

    private HttpClient httpClient;
    private String rpcUrl;

    @BeforeAll
    void initClient() throws Exception {
        log.info("[init] Starting Anvil container manually...");
        try {
            ANVIL_CONTAINER.start();
        } catch (Exception e) {
            log.error("[init] Failed to start Anvil container. Logs:\n{}", ANVIL_CONTAINER.getLogs());
            throw e;
        }
        httpClient = HttpClient.newHttpClient();
        rpcUrl = "http://" + ANVIL_CONTAINER.getHost() + ":" + ANVIL_CONTAINER.getMappedPort(8545);
        log.info("Anvil fork RPC URL: {}", rpcUrl);

        log.info("[init] CONTAINER LOGS START");
        log.info(ANVIL_CONTAINER.getLogs());
        log.info("[init] CONTAINER LOGS END");
    }

    @org.junit.jupiter.api.AfterAll
    void stopContainer() {
        if (ANVIL_CONTAINER.isRunning()) {
            ANVIL_CONTAINER.stop();
        }
    }

    @Test
    @DisplayName("Should return a valid RPC response")
    void shouldReturnAnvilClientVersion() throws Exception {
        JsonNode result = callRpc("web3_clientVersion", "[]");
        log.info("[web3_clientVersion] Response: {}", result.toPrettyString());

        assertThat(result.has("result")).isTrue();
        assertThat(result.get("result").asText()).containsIgnoringCase("anvil");
    }

    @Test
    @DisplayName("Should return Polygon Amoy chain ID")
    void shouldReturnPolygonAmoyChainId() throws Exception {
        JsonNode result = callRpc("eth_chainId", "[]");
        log.info("[eth_chainId] Response: {}", result.toPrettyString());

        assertThat(result.has("result")).isTrue();
        long chainId = Long.decode(result.get("result").asText());
        assertThat(chainId).isEqualTo(POLYGON_AMOY_CHAIN_ID);
    }

    @Test
    @DisplayName("Should return a valid block number")
    void shouldReturnBlockNumberFromFork() throws Exception {
        JsonNode result = callRpc("eth_blockNumber", "[]");
        log.info("[eth_blockNumber] Response: {}", result.toPrettyString());

        assertThat(result.has("result")).isTrue();
        String blockHex = result.get("result").asText();
        assertThat(blockHex).startsWith("0x");
        BigInteger blockNumber = new BigInteger(blockHex.substring(2), 16);
        log.info("[eth_blockNumber] Block number (decimal): {}", blockNumber);
        assertThat(blockNumber).isPositive();
    }

    @Test
    @DisplayName("Should return a valid nonce")
    void shouldReturnLatestNonceForTargetAddress() throws Exception {
        JsonNode result = callRpc("eth_getTransactionCount",
                "[\"" + TARGET_ADDRESS + "\",\"latest\"]");
        log.info("[eth_getTransactionCount latest] Response: {}", result.toPrettyString());

        assertThat(result.has("result")).isTrue();
        String nonceHex = result.get("result").asText();
        assertThat(nonceHex).startsWith("0x");
        BigInteger nonce = new BigInteger(nonceHex.substring(2), 16);
        log.info("[eth_getTransactionCount latest] Nonce for {}: {}", TARGET_ADDRESS, nonce);
        assertThat(nonce).isGreaterThanOrEqualTo(BigInteger.ZERO);
    }

    @Test
    @DisplayName("Should return a valid nonce")
    void shouldReturnPendingNonceForTargetAddress() throws Exception {
        JsonNode result = callRpc("eth_getTransactionCount",
                "[\"" + TARGET_ADDRESS + "\",\"pending\"]");
        log.info("[eth_getTransactionCount pending] Response: {}", result.toPrettyString());

        assertThat(result.has("result")).isTrue();
        String nonceHex = result.get("result").asText();
        assertThat(nonceHex).startsWith("0x");
        BigInteger pendingNonce = new BigInteger(nonceHex.substring(2), 16);
        log.info("[eth_getTransactionCount pending] Pending nonce for {}: {}", TARGET_ADDRESS, pendingNonce);
        assertThat(pendingNonce).isGreaterThanOrEqualTo(BigInteger.ZERO);
    }

    @Test
    @DisplayName("Should have a valid nonce consistency")
    void shouldHaveConsistentLatestAndPendingNonce() throws Exception {
        JsonNode latestResult = callRpc("eth_getTransactionCount",
                "[\"" + TARGET_ADDRESS + "\",\"latest\"]");
        JsonNode pendingResult = callRpc("eth_getTransactionCount",
                "[\"" + TARGET_ADDRESS + "\",\"pending\"]");

        BigInteger latestNonce = new BigInteger(latestResult.get("result").asText().substring(2), 16);
        BigInteger pendingNonce = new BigInteger(pendingResult.get("result").asText().substring(2), 16);

        log.info("[nonce consistency] latest={}, pending={}", latestNonce, pendingNonce);
        assertThat(pendingNonce).isGreaterThanOrEqualTo(latestNonce);
    }

    @Test
    @DisplayName("Should return a valid balance")
    void shouldReturnBalanceForTargetAddress() throws Exception {
        JsonNode result = callRpc("eth_getBalance",
                "[\"" + TARGET_ADDRESS + "\",\"latest\"]");
        log.info("[eth_getBalance] Response: {}", result.toPrettyString());

        assertThat(result.has("result")).isTrue();
        String balanceHex = result.get("result").asText();
        assertThat(balanceHex).startsWith("0x");
        BigInteger balance = new BigInteger(balanceHex.substring(2), 16);
        log.info("[eth_getBalance] Balance for {} (wei): {}", TARGET_ADDRESS, balance);
        assertThat(balance).isGreaterThanOrEqualTo(BigInteger.ZERO);
    }

    @Test
    @DisplayName("Should return a valid code")
    void shouldReturnCodeForTargetAddress() throws Exception {
        JsonNode result = callRpc("eth_getCode",
                "[\"" + TARGET_ADDRESS + "\",\"latest\"]");
        log.info("[eth_getCode] Response: {}", result.toPrettyString());

        assertThat(result.has("result")).isTrue();
        String code = result.get("result").asText();
        log.info("[eth_getCode] Code for {}: {} (EOA if 0x)", TARGET_ADDRESS,
                "0x".equals(code) ? "EOA (no code)" : "Contract (" + code.length() + " chars)");
    }

    @Test
    @DisplayName("Should return a valid nonce for a specific address")
    void shouldReturnLatestAndPendingNonceForTargetAddress() throws Exception {
        // Endereço específico solicitado: 0xc9019F78FE72655403A52baCA31902E8f8944a8f
        String address = "0xc9019F78FE72655403A52baCA31902E8f8944a8f";
        
        JsonNode latestNonceResult = callRpc("eth_getTransactionCount",
                "[\"" + address + "\",\"latest\"]");
        BigInteger latestNonce = new BigInteger(latestNonceResult.get("result").asText().substring(2), 16);

        JsonNode pendingNonceResult = callRpc("eth_getTransactionCount",
                "[\"" + address + "\",\"pending\"]");
        BigInteger pendingNonce = new BigInteger(pendingNonceResult.get("result").asText().substring(2), 16);

        log.info("[nonce verification] Address {}: latest={}, pending={}", address, latestNonce, pendingNonce);

        assertThat(latestNonceResult.has("result")).isTrue();
        assertThat(pendingNonceResult.has("result")).isTrue();
        // No fork inicial, eles devem ser iguais a menos que haja transações na mempool do fork
        assertThat(pendingNonce).isGreaterThanOrEqualTo(latestNonce);
    }

    @Test
    @DisplayName("Should verify fork state on Polygon Amoy")
    void shouldVerifyForkStateOnPolygonAmoy() throws Exception {
        JsonNode chainIdResult = callRpc("eth_chainId", "[]");
        long chainId = Long.decode(chainIdResult.get("result").asText());
        
        JsonNode blockNumberResult = callRpc("eth_blockNumber", "[]");
        BigInteger blockNumber = new BigInteger(blockNumberResult.get("result").asText().substring(2), 16);

        log.info("[fork state] Chain ID: {} | Block Number: {}", chainId, blockNumber);

        // Se o fork funcionou, o Chain ID deve ser 80002 e o bloco deve ser muito alto (não zero)
        assertThat(chainId).isEqualTo(80002L);
        assertThat(blockNumber).isGreaterThan(BigInteger.valueOf(30000000L));
    }

    @Test
    @DisplayName("Should verify fork is operational")
    void shouldVerifyForkIsOperational() throws Exception {
        JsonNode clientVersion = callRpc("web3_clientVersion", "[]");
        assertThat(clientVersion.get("result").asText()).containsIgnoringCase("anvil");

        JsonNode chainId = callRpc("eth_chainId", "[]");
        long chain = Long.decode(chainId.get("result").asText());
        assertThat(chain).isEqualTo(POLYGON_AMOY_CHAIN_ID);

        JsonNode blockNumber = callRpc("eth_blockNumber", "[]");
        BigInteger block = new BigInteger(blockNumber.get("result").asText().substring(2), 16);
        assertThat(block).isPositive();

        JsonNode nonce = callRpc("eth_getTransactionCount",
                "[\"" + TARGET_ADDRESS + "\",\"latest\"]");
        assertThat(nonce.has("result")).isTrue();

        log.info("[Fork verification] Anvil fork operational — chain={}, block={}, target nonce={}",
                chain, block,
                new BigInteger(nonce.get("result").asText().substring(2), 16));
    }

    private JsonNode callRpc(String method, String params) throws Exception {
        String body = String.format(
                "{\"jsonrpc\":\"2.0\",\"method\":\"%s\",\"params\":%s,\"id\":1}",
                method, params);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(rpcUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.info("[RPC] {} -> HTTP {} | Body: {}", method, response.statusCode(), response.body());

        assertThat(response.statusCode()).isEqualTo(200);
        return MAPPER.readTree(response.body());
    }
}
