package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.gateway.NetworkRepository;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenType;
import dev.bloco.wallet.hub.domain.model.network.Network;

import java.util.List;
import java.util.UUID;

import org.springframework.util.StringUtils;

/**
 * ListSupportedTokensUseCase is responsible for retrieving available tokens.
 * It provides filtering capabilities to find tokens by various criteria.
 * <p/>
 * Business Rules:
 * - Tokens can be filtered by network, type, or other criteria
 * - Only active networks' tokens are included by default
 * - Results can be paginated for large datasets
 * <p/>
 * No domain events are published by this read-only operation.
 */
public record ListSupportedTokensUseCase(
    TokenRepository tokenRepository,
    NetworkRepository networkRepository) {

    private static final String ERROR_NETWORK_ID_REQUIRED = "Network ID must be provided";
    private static final String ERROR_TOKEN_TYPE_REQUIRED = "Token type must be provided";
    private static final String ERROR_SYMBOL_REQUIRED = "Symbol must be provided";
    private static final String ERROR_NETWORK_NOT_FOUND_TEMPLATE = "Network not found with id: %s";
    private static final String ERROR_CORRELATION_REQUIRED = "Correlation ID must be provided";
    private static final String ERROR_CORRELATION_INVALID = "Correlation ID must be a valid UUID";

    /**
     * Retrieves all supported tokens.
     *
     * @return list of all available tokens
     */
    public List<Token> listAllTokens() {
        return tokenRepository.findAll();
    }

    /**
     * Retrieves tokens supported on a specific network.
     *
     * @param networkId the unique identifier of the network
     * @return list of tokens available on the network
     * @throws IllegalArgumentException if network not found
     */
    public List<Token> listTokensByNetwork(UUID networkId, String correlationId) {
        if (networkId == null) {
            throw new IllegalArgumentException(ERROR_NETWORK_ID_REQUIRED);
        }

        String normalizedCorrelation = normalizeCorrelationId(correlationId);

        networkRepository.findById(networkId, normalizedCorrelation)
                .orElseThrow(() -> new IllegalArgumentException(ERROR_NETWORK_NOT_FOUND_TEMPLATE.formatted(networkId)));

        return tokenRepository.findByNetworkId(networkId);
    }

    /**
     * Retrieves tokens of a specific type.
     *
     * @param tokenType the type of tokens to retrieve
     * @return list of tokens of the specified type
     * @throws IllegalArgumentException if token type is null
     */
    public List<Token> listTokensByType(TokenType tokenType) {
        if (tokenType == null) {
            throw new IllegalArgumentException(ERROR_TOKEN_TYPE_REQUIRED);
        }

        return tokenRepository.findByType(tokenType);
    }

    /**
     * Retrieves native tokens for all networks.
     *
     * @return list of native tokens
     */
    public List<Token> listNativeTokens() {
        return tokenRepository.findByType(TokenType.NATIVE);
    }

    /**
     * Retrieves ERC20 tokens.
     *
     * @return list of ERC20 tokens
     */
    public List<Token> listERC20Tokens() {
        return tokenRepository.findByType(TokenType.ERC20);
    }

    /**
     * Retrieves NFT tokens (ERC721 and ERC1155).
     *
     * @return list of NFT tokens
     */
    public List<Token> listNFTTokens() {
        List<Token> erc721Tokens = tokenRepository.findByType(TokenType.ERC721);
        List<Token> erc1155Tokens = tokenRepository.findByType(TokenType.ERC1155);
        
        java.util.List<Token> nftTokens = new java.util.ArrayList<>(erc721Tokens);
        nftTokens.addAll(erc1155Tokens);
        
        return nftTokens;
    }

    /**
     * Searches for tokens by symbol.
     *
     * @param symbol the token symbol to search for
     * @return list of tokens matching the symbol
     * @throws IllegalArgumentException if symbol is null or empty
     */
    public List<Token> searchTokensBySymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new IllegalArgumentException(ERROR_SYMBOL_REQUIRED);
        }

        return tokenRepository.findBySymbol(symbol.trim().toUpperCase());
    }

    /**
     * Retrieves tokens supported on active networks only.
     *
     * @return list of tokens on active networks
     */
    public List<Token> listTokensOnActiveNetworks(String correlationId) {
        List<Network> activeNetworks = networkRepository.findAll(normalizeCorrelationId(correlationId)).stream()
                .filter(Network::isAvailable)
                .toList();

        return activeNetworks.stream()
                .flatMap(network -> tokenRepository.findByNetworkId(network.getId()).stream())
                .distinct()
                .toList();
    }

    /**
     * Gets comprehensive token information for listing.
     *
     * @param networkId optional network filter
     * @param tokenType optional token type filter
     * @return structured token listing information
     */
    public TokenListingResult getTokenListing(UUID networkId, TokenType tokenType, String correlationId) {
        List<Token> tokens;
        String normalizedCorrelation = correlationId != null ? normalizeCorrelationId(correlationId) : null;

        if (networkId != null && tokenType != null) {
            // Filter by both network and type
            tokens = tokenRepository.findByNetworkId(networkId).stream()
                    .filter(token -> token.getType() == tokenType)
                    .toList();
        } else if (networkId != null) {
            // Filter by network only
            tokens = listTokensByNetwork(networkId, normalizedCorrelation);
        } else if (tokenType != null) {
            // Filter by type only
            tokens = listTokensByType(tokenType);
        } else {
            // No filters
            tokens = listAllTokens();
        }

        // Group tokens by type for summary
        long nativeCount = tokens.stream().filter(token -> token.getType() == TokenType.NATIVE).count();
        long erc20Count = tokens.stream().filter(token -> token.getType() == TokenType.ERC20).count();
        long nftCount = tokens.stream().filter(Token::isNFT).count();
        long customCount = tokens.stream().filter(token -> token.getType() == TokenType.CUSTOM).count();

        return new TokenListingResult(
            tokens,
            tokens.size(),
            nativeCount,
            erc20Count,
            nftCount,
            customCount
        );
    }

    /**
     * Result containing token listing with summary information.
     */
    public record TokenListingResult(
        List<Token> tokens,
        int totalCount,
        long nativeTokens,
        long erc20Tokens,
        long nftTokens,
        long customTokens
    ) {}

    private String normalizeCorrelationId(String correlationId) {
        if (!StringUtils.hasText(correlationId)) {
            throw new IllegalArgumentException(ERROR_CORRELATION_REQUIRED);
        }

        try {
            UUID parsed = UUID.fromString(correlationId.trim());
            return parsed.toString();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ERROR_CORRELATION_INVALID, ex);
        }
    }
}