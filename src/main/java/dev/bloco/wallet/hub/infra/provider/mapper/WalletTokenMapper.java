package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.model.wallet.WalletToken;
import dev.bloco.wallet.hub.infra.provider.data.entity.WalletTokenEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WalletTokenMapper {
    default WalletToken toDomain(WalletTokenEntity entity) {
        if (entity == null) return null;
        WalletToken walletToken = WalletToken.create(
                entity.getId(),
                entity.getWalletId(),
                entity.getTokenId(),
                entity.getDisplayName()
        );
        if (!entity.isEnabled()) walletToken.disable();
        if (!entity.isVisible()) walletToken.hide();
        return walletToken;
    }

    default WalletTokenEntity toEntity(WalletToken domain) {
        if (domain == null) return null;
        WalletTokenEntity entity = new WalletTokenEntity();
        entity.setId(domain.getId());
        entity.setWalletId(domain.getWalletId());
        entity.setTokenId(domain.getTokenId());
        entity.setAddedAt(domain.getAddedAt());
        entity.setEnabled(domain.isEnabled());
        entity.setDisplayName(domain.getDisplayName());
        entity.setVisible(domain.isVisible());
        return entity;
    }
}
