package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.Transaction;
import dev.bloco.wallet.hub.infra.provider.data.entity.TransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

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
