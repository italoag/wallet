package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.domain.model.Wallet;
import dev.bloco.wallet.hub.domain.model.wallet.WalletStatus;
import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.WalletMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JpaWalletRepository is an implementation of the WalletRepository interface
 * that provides database operations for managing wallet data using JPA.
 *<p/>
 * This class acts as an intermediary between the domain layer and the persistence
 * layer, delegating database operations to the SpringDataWalletRepository and
 * using the WalletMapper for mapping between domain and entity objects.
 *<p/>
 * Responsibilities include:
 * - Locating wallets in the database by their unique identifier (UUID)
 * - Saving new wallet records
 * - Updating existing wallet records
 *<p/>
 * Dependencies:
 * - SpringDataWalletRepository: A JPA repository interface for database operations.
 * - WalletMapper: A mapper interface that translates Wallet domain objects
 *   to WalletEntity database models and vice versa.
 *<p/>
 * Annotated with @Repository to denote its role in the persistence layer and to
 * allow Spring's component scanning to discover and manage the bean.
 */
@Repository
public class JpaWalletRepository implements WalletRepository {
    private final SpringDataWalletRepository springDataWalletRepository;
    private final WalletMapper walletMapper;

    public JpaWalletRepository(SpringDataWalletRepository springDataWalletRepository, WalletMapper walletMapper) {
        this.springDataWalletRepository = springDataWalletRepository;
        this.walletMapper = walletMapper;
    }

    /**
     * Retrieves a wallet by its unique identifier (UUID) from the database.
     * The method returns an Optional containing the wallet if found, or an empty Optional
     * if no wallet exists with the given identifier.
     *
     * @param id the unique identifier (UUID) of the wallet to be retrieved
     * @return an Optional containing the wallet represented as a domain object
     *         if found, or an empty Optional if the wallet does not exist
     */
    @Override
    public Optional<Wallet> findById(UUID id) {
        return springDataWalletRepository.findById(id)
                .map(walletMapper::toDomain);
    }

  /**
   * Saves a wallet into the database. This involves converting the given wallet domain object
   * to a wallet entity, persisting it using the repository, and mapping the saved entity
   * back to a domain object.
   *
   * @param wallet the wallet domain object to be saved. It contains information about the
   *               wallet such as its ID, user ID, and balance.
   * @return the saved wallet as a domain object. The returned wallet contains the
   *         updated state of the wallet after being persisted in the database.
   */
  @Override
    public Wallet save(Wallet wallet) {
        WalletEntity entity = walletMapper.toEntity(wallet);
        return walletMapper.toDomain(springDataWalletRepository.save(entity));
    }

  /**
   * Updates an existing wallet in the database. The method takes a wallet domain object,
   * converts it to its corresponding entity representation, and saves it to the database.
   * This operation overwrites the existing database record for the wallet with the
   * same identifier, ensuring the stored data matches the provided domain object.
   *
   * @param wallet the wallet domain object containing updated information.
   *               It must include a valid unique identifier (UUID) matching an
   *               existing wallet record in the database.
   */
  @Override
    public void update(Wallet wallet) {
        WalletEntity entity = walletMapper.toEntity(wallet);
        springDataWalletRepository.save(entity);
    }

    @Override
    public List<Wallet> findAll() {
        return springDataWalletRepository.findAll().stream()
                .map(walletMapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        springDataWalletRepository.deleteById(id);
    }

    @Override
    public List<Wallet> findByName(String name) {
        // No name in WalletEntity schema; fallback to returning all
        return findAll().stream()
                .filter(w -> name == null || name.equals(w.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataWalletRepository.existsById(id);
    }

    @Override
    public List<Wallet> findByUserId(UUID userId) {
        // Fallback implementation - filter all wallets by userId
        return findAll().stream()
                .filter(wallet -> userId != null && userId.equals(wallet.getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Wallet> findByUserIdAndStatus(UUID userId, WalletStatus status) {
        // Fallback implementation - filter by userId and status
        return findAll().stream()
                .filter(wallet -> userId != null && userId.equals(wallet.getUserId())
                    && status != null && status.equals(wallet.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public List<Wallet> findActiveByUserId(UUID userId) {
        return findByUserIdAndStatus(userId, WalletStatus.ACTIVE);
    }
}
