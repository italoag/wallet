package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.User;

import dev.bloco.wallet.hub.infra.provider.data.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "name", source = "name"),
            @Mapping(target = "email", source = "email")
    })
    User toDomain(UserEntity entity);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "name", source = "name"),
            @Mapping(target = "email", source = "email")
    })
    UserEntity toEntity(User domain);
}
