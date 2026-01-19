package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.model.user.UserSession;
import dev.bloco.wallet.hub.infra.provider.data.entity.UserSessionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserSessionMapper {
    default UserSession toDomain(UserSessionEntity entity) {
        if (entity == null) return null;
        return UserSession.create(
                entity.getId(),
                entity.getUserId(),
                entity.getToken(),
                entity.getExpiresAt()
        );
    }

    default UserSessionEntity toEntity(UserSession domain) {
        if (domain == null) return null;
        UserSessionEntity entity = new UserSessionEntity();
        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setToken(domain.getToken());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setActive(domain.isActive());
        return entity;
    }
}
