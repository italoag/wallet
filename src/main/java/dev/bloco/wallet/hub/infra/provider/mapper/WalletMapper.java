package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "userId", source = "userId"),
            @Mapping(target = "balance", source = "balance")
    })
    Wallet toDomain(WalletEntity entity);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "userId", source = "userId"),
            @Mapping(target = "balance", source = "balance")
    })
    WalletEntity toEntity(Wallet domain);
}
