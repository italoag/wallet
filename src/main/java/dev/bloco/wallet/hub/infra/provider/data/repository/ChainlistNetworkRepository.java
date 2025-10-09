package dev.bloco.wallet.hub.infra.provider.data.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.domain.model.network.NetworkStatus;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * {@link NetworkRepository} implementation that retrieves network metadata dynamically from
 * the Chainlist service. The repository keeps a short-lived cache to avoid overwhelming the
 * remote service and also supports locally added networks stored in-memory.
 */
@Repository
public class ChainlistNetworkRepository implements NetworkRepository {

    static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final Logger LOGGER = LoggerFactory.getLogger(ChainlistNetworkRepository.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String chainlistUrl;
    private final Duration cacheTtl;

    private final AtomicReference<List<Network>> cachedRemoteNetworks = new AtomicReference<>(List.of());
    private final ConcurrentMap<UUID, Network> customNetworks = new ConcurrentHashMap<>();
    private volatile Instant cacheExpiresAt = Instant.EPOCH;

    public ChainlistNetworkRepository(
            WebClient.Builder webClientBuilder,
            ObjectMapper objectMapper,
            @Value("${wallet.networks.chainlist-url:}") String chainlistUrl,
            @Value("${wallet.networks.cache-ttl:PT5M}") Duration cacheTtl) {
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB limit
                .build();
        this.objectMapper = objectMapper;
        this.chainlistUrl = chainlistUrl;
        this.cacheTtl = cacheTtl.isNegative() ? Duration.ZERO : cacheTtl;
    }

    @Override
    public Network save(Network network, String correlationId) {
        customNetworks.put(network.getId(), network);
        return network;
    }

    @Override
    public Optional<Network> findById(UUID id, String correlationId) {
        Network custom = customNetworks.get(id);
        if (custom != null) {
            return Optional.of(custom);
        }

        return getRemoteNetworks(correlationId).stream()
                .filter(network -> network.getId().equals(id))
                .findFirst();
    }

    @Override
    public List<Network> findAll(String correlationId) {
        Map<UUID, Network> combined = new LinkedHashMap<>();
        getRemoteNetworks(correlationId).forEach(network -> combined.put(network.getId(), network));
        customNetworks.values().forEach(network -> combined.put(network.getId(), network));
        return List.copyOf(combined.values());
    }

    @Override
    public void delete(UUID id, String correlationId) {
        customNetworks.remove(id);
    }

    @Override
    public Optional<Network> findByChainId(String chainId, String correlationId) {
        Objects.requireNonNull(chainId, "chainId must not be null");

        return findAll(correlationId).stream()
                .filter(network -> chainId.equalsIgnoreCase(network.getChainId()))
                .findFirst();
    }

    @Override
    public List<Network> findByStatus(NetworkStatus status, String correlationId) {
        Objects.requireNonNull(status, "status must not be null");

        return findAll(correlationId).stream()
                .filter(network -> network.getStatus() == status)
                .toList();
    }

    @Override
    public List<Network> findByName(String name, String correlationId) {
        Objects.requireNonNull(name, "name must not be null");

        String normalized = name.toLowerCase();
        return findAll(correlationId).stream()
                .filter(network -> network.getName().toLowerCase().contains(normalized))
                .toList();
    }

    @Override
    public boolean existsById(UUID id, String correlationId) {
        return findById(id, correlationId).isPresent();
    }

    @Override
    public boolean existsByChainId(String chainId, String correlationId) {
        return findByChainId(chainId, correlationId).isPresent();
    }

    private List<Network> getRemoteNetworks(String correlationId) {
        Instant now = Instant.now();
        List<Network> cached = cachedRemoteNetworks.get();
        if (!cacheExpired(now, cached)) {
            return cached;
        }

        synchronized (this) {
            cached = cachedRemoteNetworks.get();
            if (!cacheExpired(now, cached)) {
                return cached;
            }

            List<Network> refreshed = fetchFromChainlist(correlationId);
            if (!refreshed.isEmpty()) {
                cachedRemoteNetworks.set(refreshed);
                cacheExpiresAt = now.plus(cacheTtl);
                return refreshed;
            }

            // When refresh fails, keep serving the existing cache, even if stale
            return cached;
        }
    }

    private boolean cacheExpired(Instant now, List<Network> cached) {
        return now.isAfter(cacheExpiresAt) || cached.isEmpty();
    }

    private List<Network> fetchFromChainlist(String correlationId) {
        try {
            Mono<String> responseMono = webClient.get()
                    .uri(chainlistUrl)
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> applyCorrelation(headers, correlationId))
                    .retrieve()
                    .bodyToMono(String.class);

            return responseMono
                    .timeout(DEFAULT_TIMEOUT)
                    .map(payload -> {
                        if (!StringUtils.hasText(payload)) {
                            LOGGER.warn("Empty response when fetching networks from Chainlist");
                            return List.<Network>of();
                        }
                        
                        try {
                            JsonNode root = objectMapper.readTree(payload);
                            if (!root.isArray()) {
                                LOGGER.warn("Unexpected Chainlist payload format: not an array");
                                return List.<Network>of();
                            }

                            List<Network> networks = new ArrayList<>();
                            for (JsonNode node : root) {
                                mapToNetwork(node).ifPresent(networks::add);
                            }
                            return networks;
                        } catch (JsonProcessingException ex) {
                            LOGGER.error("Failed to parse Chainlist response", ex);
                            return List.<Network>of();
                        }
                    })
                    .doOnError(ex -> {
                        if (ex instanceof WebClientResponseException wcEx) {
                            LOGGER.error("Chainlist responded with {} when fetching networks", wcEx.getStatusCode(), wcEx);
                        } else {
                            LOGGER.error("Unexpected error fetching networks from Chainlist", ex);
                        }
                    })
                    .onErrorReturn(List.of())
                    .subscribeOn(Schedulers.boundedElastic())
                    .block();
        } catch (Exception ex) {
            LOGGER.error("Unexpected error in fetchFromChainlist", ex);
            return List.of();
        }
    }

    private void applyCorrelation(HttpHeaders headers, String correlationId) {
        if (StringUtils.hasText(correlationId)) {
            headers.set(CORRELATION_HEADER, correlationId);
        }
    }

    private Optional<Network> mapToNetwork(JsonNode node) {
        String name = node.path("name").asText(null);
        JsonNode chainIdNode = node.path("chainId");
        if (!StringUtils.hasText(name) || chainIdNode.isMissingNode()) {
            return Optional.empty();
        }

        String chainId = chainIdNode.asText();
        String rpcUrl = extractRpcUrl(node.path("rpc"));
        if (rpcUrl == null) {
            rpcUrl = "https://chainlist.org/chain/" + chainId; // fallback URL
        }
        
        String explorerUrl = extractExplorerUrl(node.path("explorers")).orElse(rpcUrl);
        
        // Add a timestamp or random component to prevent collisions
        String uniqueInput = "chainlist:" + chainId + ":" + System.currentTimeMillis();
        UUID id = UUID.nameUUIDFromBytes(uniqueInput.getBytes(StandardCharsets.UTF_8));

        Network network = Network.create(id, name, chainId, rpcUrl, explorerUrl);
        return Optional.of(network);
    }

    private String extractRpcUrl(JsonNode rpcNode) {
        if (!rpcNode.isArray()) {
            return null;
        }

        for (JsonNode entry : rpcNode) {
            JsonNode urlNode = entry.has("url") ? entry.get("url") : entry;
            if (urlNode != null && urlNode.isTextual()) {
                String candidate = urlNode.asText();
                if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private Optional<String> extractExplorerUrl(JsonNode explorersNode) {
        if (!explorersNode.isArray()) {
            return Optional.empty();
        }

        for (JsonNode explorer : explorersNode) {
            JsonNode urlNode = explorer.get("url");
            if (urlNode != null && urlNode.isTextual()) {
                String url = urlNode.asText();
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return Optional.of(url);
                }
            }
        }

        return Optional.empty();
    }
}
