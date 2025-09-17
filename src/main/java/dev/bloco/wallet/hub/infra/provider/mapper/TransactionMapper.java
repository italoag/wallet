package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "fromWalletId", source = "fromWalletId"),
            @Mapping(target = "toWalletId", source = "toWalletId"),
            @Mapping(target = "amount", source = "amount"),
            @Mapping(target = "timestamp", source = "timestamp"),
            @Mapping(target = "type", source = "type")
    })
    Transaction toDomain(TransactionEntity entity);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "fromWalletId", source = "fromWalletId"),
            @Mapping(target = "toWalletId", source = "toWalletId"),
            @Mapping(target = "amount", source = "amount"),
            @Mapping(target = "timestamp", source = "timestamp"),
            @Mapping(target = "type", source = "type")
    })
    TransactionEntity toEntity(Transaction domain);
}
