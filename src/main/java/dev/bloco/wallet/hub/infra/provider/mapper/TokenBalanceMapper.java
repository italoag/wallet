package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.model.token.TokenBalance;
import dev.bloco.wallet.hub.infra.provider.data.entity.TokenBalanceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TokenBalanceMapper {
    default TokenBalance toDomain(TokenBalanceEntity entity) {
        if (entity == null) return null;
        return TokenBalance.create(
                entity.getId(),
                entity.getAddressId(),
                entity.getTokenId(),
                entity.getBalance()
        );
    }

    default TokenBalanceEntity toEntity(TokenBalance domain) {
        if (domain == null) return null;
        TokenBalanceEntity entity = new TokenBalanceEntity();
        entity.setId(domain.getId());
        entity.setAddressId(domain.getAddressId());
        entity.setTokenId(domain.getTokenId());
        entity.setBalance(domain.getBalance());
        entity.setLastUpdated(domain.getLastUpdated());
        return entity;
    }
}
