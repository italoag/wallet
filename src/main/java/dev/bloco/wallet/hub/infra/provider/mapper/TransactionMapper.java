package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.domain.model.transaction.TransactionHash;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import org.mapstruct.Mapper;
import java.math.BigDecimal;

@Mapper(componentModel = "spring")
public interface TransactionMapper {
    default Transaction toDomain(TransactionEntity entity) {
        if (entity == null)
            return null;
        return Transaction.rehydrate(
                entity.getId(),
                entity.getNetworkId(),
                new TransactionHash(entity.getHash()),
                entity.getFromAddress(),
                entity.getToAddress(),
                entity.getValue() != null ? entity.getValue() : BigDecimal.ZERO,
                entity.getData(),
                entity.getTimestamp(),
                entity.getBlockNumber(),
                entity.getBlockHash(),
                entity.getStatus(),
                entity.getGasPrice(),
                entity.getGasLimit(),
                entity.getGasUsed());
    }

    default TransactionEntity toEntity(Transaction domain) {
        if (domain == null)
            return null;
        TransactionEntity entity = new TransactionEntity();
        entity.setId(domain.getId());
        entity.setNetworkId(domain.getNetworkId());
        entity.setHash(domain.getHash());
        entity.setFromAddress(domain.getFromAddress());
        entity.setToAddress(domain.getToAddress());
        entity.setValue(domain.getValue());
        entity.setTimestamp(domain.getTimestamp());
        entity.setStatus(domain.getStatus());
        return entity;
    }
}
