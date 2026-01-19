package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.model.transaction.TransactionFee;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionFeeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionFeeMapper {
    default TransactionFee toDomain(TransactionFeeEntity entity) {
        if (entity == null)
            return null;
        return TransactionFee.create(
                entity.getId(),
                entity.getNetworkId(),
                entity.getFeeLevel(),
                entity.getGasPrice(),
                null, // baseFee not persisted
                entity.getPriorityFee(),
                true // assuming persisted fees are estimates
        );
    }

    default TransactionFeeEntity toEntity(TransactionFee domain) {
        if (domain == null)
            return null;
        TransactionFeeEntity entity = new TransactionFeeEntity();
        entity.setId(domain.getId());
        entity.setNetworkId(domain.getNetworkId());
        entity.setFeeLevel(domain.getLevel());
        entity.setGasPrice(domain.getGasPrice());
        entity.setPriorityFee(domain.getPriorityFee());
        entity.setTimestamp(domain.getTimestamp());
        return entity;
    }
}
