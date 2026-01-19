package dev.bloco.wallet.hub.infra.provider.repository;

import dev.bloco.wallet.hub.domain.gateway.WalletTokenRepository;
import dev.bloco.wallet.hub.domain.model.wallet.WalletToken;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataWalletTokenRepository;
import dev.bloco.wallet.hub.infra.provider.mapper.WalletTokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Transactional
public class JpaWalletTokenRepository implements WalletTokenRepository {
    private final SpringDataWalletTokenRepository springDataRepository;
    private final WalletTokenMapper mapper;

    @Override
    public WalletToken save(WalletToken walletToken) {
        var entity = mapper.toEntity(walletToken);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WalletToken> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletToken> findAll() {
        return springDataRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        springDataRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletToken> findByWalletId(UUID walletId) {
        return springDataRepository.findByWalletId(walletId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletToken> findByTokenId(UUID tokenId) {
        return springDataRepository.findByTokenId(tokenId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WalletToken> findByWalletIdAndTokenId(UUID walletId, UUID tokenId) {
        return springDataRepository.findByWalletIdAndTokenId(walletId, tokenId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletToken> findEnabledByWalletId(UUID walletId) {
        return springDataRepository.findByWalletIdAndIsEnabled(walletId, true).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletToken> findVisibleByWalletId(UUID walletId) {
        return springDataRepository.findByWalletIdAndIsEnabled(walletId, true).stream()
                .filter(entity -> entity.isVisible())
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByWalletIdAndTokenId(UUID walletId, UUID tokenId) {
        return springDataRepository.findByWalletIdAndTokenId(walletId, tokenId).isPresent();
    }

    @Override
    public void update(WalletToken walletToken) {
        save(walletToken);
    }
}
