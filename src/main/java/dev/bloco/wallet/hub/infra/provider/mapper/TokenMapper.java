package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.model.token.Token;
import dev.bloco.wallet.hub.infra.provider.data.entity.TokenEntity;
import org.mapstruct.Mapper;

/**
 * Mapper for converting between Token domain objects and TokenEntity database
 * entities.
 */
@Mapper(componentModel = "spring")
public interface TokenMapper {

    default Token toDomain(TokenEntity entity) {
        if (entity == null)
            return null;

        Token token = Token.create(
                entity.getId(),
                entity.getNetworkId(),
                entity.getName(),
                entity.getSymbol(),
                entity.getDecimals(),
                entity.getType(),
                entity.getContractAddress());

        // Clear creation event since this is rehydration
        token.clearEvents();

        return token;
    }

    default TokenEntity toEntity(Token domain) {
        if (domain == null)
            return null;

        TokenEntity entity = new TokenEntity();
        entity.setId(domain.getId());
        entity.setNetworkId(domain.getNetworkId());
        entity.setContractAddress(domain.getContractAddress());
        entity.setName(domain.getName());
        entity.setSymbol(domain.getSymbol());
        entity.setDecimals(domain.getDecimals());
        entity.setType(domain.getType());

        return entity;
    }
}
