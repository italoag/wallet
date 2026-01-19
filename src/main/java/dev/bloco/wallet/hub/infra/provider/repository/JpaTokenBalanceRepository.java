package dev.bloco.wallet.hub.infra.provider.repository;

import dev.bloco.wallet.hub.domain.gateway.TokenBalanceRepository;
import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataTokenBalanceRepository;
import dev.bloco.wallet.hub.infra.provider.mapper.TokenBalanceMapper;
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
public class JpaTokenBalanceRepository implements TokenBalanceRepository {
    private final SpringDataTokenBalanceRepository springDataRepository;
    private final TokenBalanceMapper mapper;

    @Override
    public TokenBalance save(TokenBalance tokenBalance) {
        var entity = mapper.toEntity(tokenBalance);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TokenBalance> findById(UUID id) {
        return springDataRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenBalance> findAll() {
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
    public List<TokenBalance> findByAddressId(UUID addressId) {
        return springDataRepository.findByAddressId(addressId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TokenBalance> findByTokenId(UUID tokenId) {
        return springDataRepository.findByTokenId(tokenId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TokenBalance> findByAddressIdAndTokenId(UUID addressId, UUID tokenId) {
        return springDataRepository.findByAddressIdAndTokenId(addressId, tokenId)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return springDataRepository.existsById(id);
    }
}
