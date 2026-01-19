package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.model.address.*;
import dev.bloco.wallet.hub.infra.provider.data.entity.AddressEntity;
import org.mapstruct.Mapper;

/**
 * Mapper for converting between Address domain objects and AddressEntity
 * database entities.
 */
@Mapper(componentModel = "spring")
public interface AddressMapper {

    default Address toDomain(AddressEntity entity) {
        if (entity == null)
            return null;

        Address address = Address.create(
                entity.getId(),
                entity.getWalletId(),
                entity.getNetworkId(),
                new PublicKey(entity.getPublicKey()),
                new AccountAddress(entity.getAccountAddress()),
                entity.getType(),
                entity.getDerivationPath());

        // Clear events from creation since this is rehydration
        address.clearEvents();

        // Restore status if different from default
        if (entity.getStatus() == AddressStatus.ARCHIVED) {
            address.archive();
            address.clearEvents(); // Clear the status change event too
        }

        // Restore collections
        entity.getTransactionIds().forEach(address::addTransaction);
        entity.getTokenBalanceIds().forEach(address::addTokenBalance);
        entity.getOwnedContractIds().forEach(address::addOwnedContract);

        return address;
    }

    default AddressEntity toEntity(Address domain) {
        if (domain == null)
            return null;

        AddressEntity entity = new AddressEntity();
        entity.setId(domain.getId());
        entity.setWalletId(domain.getWalletId());
        entity.setNetworkId(domain.getNetworkId());
        entity.setPublicKey(domain.getPublicKey().getValue());
        entity.setAccountAddress(domain.getAccountAddress().getValue());
        entity.setType(domain.getType());
        entity.setDerivationPath(domain.getDerivationPath());
        entity.setStatus(domain.getStatus());
        entity.getTransactionIds().addAll(domain.getTransactionIds());
        entity.getTokenBalanceIds().addAll(domain.getTokenBalanceIds());
        entity.getOwnedContractIds().addAll(domain.getOwnedContractIds());

        return entity;
    }
}
