package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.model.token.TokenType;
import dev.bloco.wallet.hub.infra.provider.data.entity.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for TokenEntity.
 */
@Repository
public interface SpringDataTokenRepository extends JpaRepository<TokenEntity, UUID> {

    List<TokenEntity> findByNetworkId(UUID networkId);

    Optional<TokenEntity> findByNetworkIdAndContractAddress(UUID networkId, String contractAddress);

    List<TokenEntity> findByType(TokenType type);

    List<TokenEntity> findBySymbol(String symbol);
}
