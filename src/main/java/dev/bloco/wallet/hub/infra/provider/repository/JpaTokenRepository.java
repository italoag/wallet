package dev.bloco.wallet.hub.infra.provider.repository;

import dev.bloco.wallet.hub.domain.gateway.TokenRepository;
import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.domain.model.token.TokenType;
import dev.bloco.wallet.hub.infra.provider.data.repository.SpringDataTokenRepository;
import dev.bloco.wallet.hub.infra.provider.mapper.TokenMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * JPA implementation of TokenRepository.
 */
@Repository
@RequiredArgsConstructor
@Transactional
public class JpaTokenRepository implements TokenRepository {

    private final SpringDataTokenRepository springDataRepository;
    private final TokenMapper mapper;

    @Override
    public Token save(Token token) {
        var entity = mapper.toEntity(token);
        var savedEntity = springDataRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Token> findById(UUID id) {
        return springDataRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Token> findAll() {
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
    public List<Token> findByNetworkId(UUID networkId) {
        return springDataRepository.findByNetworkId(networkId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Token> findByNetworkIdAndContractAddress(UUID networkId, String contractAddress) {
        return springDataRepository.findByNetworkIdAndContractAddress(networkId, contractAddress)
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Token> findByType(TokenType type) {
        return springDataRepository.findByType(type).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Token> findBySymbol(String symbol) {
        return springDataRepository.findBySymbol(symbol).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UUID id) {
        return springDataRepository.existsById(id);
    }
}
