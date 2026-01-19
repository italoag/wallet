package dev.bloco.wallet.hub.infra.provider.mapper;

import dev.bloco.wallet.hub.domain.model.network.Network;
import dev.bloco.wallet.hub.infra.provider.data.entity.NetworkEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NetworkMapper {
    default Network toDomain(NetworkEntity entity) {
        if (entity == null) return null;
        Network network = Network.create(
                entity.getId(),
                entity.getName(),
                entity.getChainId(),
                entity.getRpcUrl(),
                entity.getExplorerUrl()
        );
        network.clearEvents();
        return network;
    }

    default NetworkEntity toEntity(Network domain) {
        if (domain == null) return null;
        NetworkEntity entity = new NetworkEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setChainId(domain.getChainId());
        entity.setRpcUrl(domain.getRpcUrl());
        entity.setExplorerUrl(domain.getExplorerUrl());
        entity.setStatus(domain.getStatus());
        return entity;
    }
}
