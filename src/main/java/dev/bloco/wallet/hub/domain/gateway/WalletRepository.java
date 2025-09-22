package dev.bloco.wallet.hub.domain.gateway;

import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.wallet.WalletStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * WalletRepository defines the interface for managing wallet entities in the system.
 * Implementations of this interface handle the persistence, retrieval, and update
 * of wallet data, enabling interaction with wallets within the domain.
 *<p/>
 * A wallet is uniquely identified by a UUID and is associated with a specific user.
 * This repository provides methods to find a wallet by its identifier, persist a new
 * wallet, and update an existing wallet's information.
 */
public interface WalletRepository {

    Wallet save(Wallet wallet);

    void update(Wallet wallet);
    
    Optional<Wallet> findById(UUID id);
    
    List<Wallet> findAll();
    
    void delete(UUID id);
    
    List<Wallet> findByName(String name);
    
    boolean existsById(UUID id);
    
    List<Wallet> findByUserId(UUID userId);
    
    List<Wallet> findByUserIdAndStatus(UUID userId, WalletStatus status);
    
    List<Wallet> findActiveByUserId(UUID userId);
}