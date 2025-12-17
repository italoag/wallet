package dev.bloco.wallet.hub.infra.provider.mapper;

import org.mapstruct.Mapper;

import dev.bloco.wallet.hub.domain.model.transaction.Transaction;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;

/**
 * Mapper interface for converting between {@link Transaction} domain objects and
 * {@link TransactionEntity} database entities, and vice versa.
 *<p/>
 * This mapper is responsible for mapping the various fields of the Transaction
 * and TransactionEntity classes, ensuring consistency and simplifying the process
 * of transferring data between the domain and persistence layers.
 *<p/>
 * The mapping is bidirectional, enabling:
 * - Conversion from a {@link TransactionEntity} to a {@link Transaction} domain object.
 * - Conversion from a {@link Transaction} domain object to a {@link TransactionEntity}.
 *<p/>
 * The mapper leverages the MapStruct framework to generate the implementation
 * of the mapping process, minimizing manual boilerplate code and ensuring an efficient
 * and maintainable transformation between the two layers.
 *<p/>
 * Notes:
 * - Each field in both the domain and entity objects is explicitly mapped to its counterpart,
 *   ensuring accuracy in data conversion.
 * - This mapper is designed to integrate seamlessly with Spring's dependency injection framework,
 *   as indicated by the use of `componentModel = "spring"`.
 *<p/>
 * Use this interface within services or repositories to handle conversions between the
 * domain model and database entity for transactions.
 */
@Mapper(componentModel = "spring")
public interface TransactionMapper {

    default Transaction toDomain(TransactionEntity entity) {
        if (entity == null) return null;
        return Transaction.rehydrate(
                entity.getId(),
                entity.getNetworkId(),
                new dev.bloco.wallet.hub.domain.model.transaction.TransactionHash(entity.getHash()),
                entity.getFromAddress(),
                entity.getToAddress(),
                entity.getValue(),
                entity.getData(),
                entity.getTimestamp(),
                entity.getBlockNumber(),
                entity.getBlockHash(),
                entity.getStatus(),
                entity.getGasPrice(),
                entity.getGasLimit(),
                entity.getGasUsed()
        );
    }

    default TransactionEntity toEntity(Transaction domain) {
        if (domain == null) return null;
        TransactionEntity entity = new TransactionEntity();
        entity.setId(domain.getId());
        entity.setNetworkId(domain.getNetworkId());
        entity.setHash(domain.getHash());
        entity.setFromAddress(domain.getFromAddress());
        entity.setToAddress(domain.getToAddress());
        entity.setValue(domain.getValue());
        entity.setGasPrice(domain.getGasPrice());
        entity.setGasLimit(domain.getGasLimit());
        entity.setGasUsed(domain.getGasUsed());
        entity.setData(domain.getData());
        entity.setTimestamp(domain.getTimestamp());
        entity.setBlockNumber(domain.getBlockNumber());
        entity.setBlockHash(domain.getBlockHash());
        entity.setStatus(domain.getStatus());
        return entity;
    }
}
