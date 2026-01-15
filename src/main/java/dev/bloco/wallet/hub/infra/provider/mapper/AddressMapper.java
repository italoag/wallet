package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.model.address.AccountAddress;
import dev.bloco.wallet.hub.domain.model.address.Address;
import dev.bloco.wallet.hub.domain.model.address.PublicKey;
import dev.bloco.wallet.hub.infra.provider.data.entity.AddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(target = "publicKey", source = "publicKey", qualifiedByName = "toPublicKey")
    @Mapping(target = "accountAddress", source = "accountAddress", qualifiedByName = "toAccountAddress")
    Address toDomain(AddressEntity entity);

    @Mapping(target = "publicKey", source = "publicKey.value")
    @Mapping(target = "accountAddress", source = "accountAddress.value")
    AddressEntity toEntity(Address domain);

    @Named("toPublicKey")
    default PublicKey toPublicKey(String value) {
        return value != null ? new PublicKey(value) : null;
    }

    @Named("toAccountAddress")
    default AccountAddress toAccountAddress(String value) {
        return value != null ? new AccountAddress(value) : null;
    }
}
