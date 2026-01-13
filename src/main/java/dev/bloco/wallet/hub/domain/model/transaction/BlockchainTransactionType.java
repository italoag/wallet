package dev.bloco.wallet.hub.domain.model.transaction;

/**
 * Transaction types for gas estimation.
 * Represents different categories of blockchain transactions,
 * each with typical gas consumption characteristics.
 */
public enum BlockchainTransactionType {
    /**
     * Simple native token transfer (e.g., ETH transfer).
     * Typical gas: 21,000
     */
    SIMPLE_TRANSFER,
    
    /**
     * ERC20 token transfer.
     * Typical gas: 65,000
     */
    ERC20_TRANSFER,
    
    /**
     * ERC721 (NFT) token transfer.
     * Typical gas: 85,000
     */
    ERC721_TRANSFER,
    
    /**
     * Smart contract interaction (function call).
     * Typical gas: 150,000
     */
    CONTRACT_INTERACTION,
    
    /**
     * Smart contract deployment.
     * Typical gas: 500,000
     */
    CONTRACT_DEPLOYMENT,
    
    /**
     * Complex DeFi operations (swaps, liquidity provision, etc.).
     * Typical gas: 300,000
     */
    COMPLEX_DEFI
}
