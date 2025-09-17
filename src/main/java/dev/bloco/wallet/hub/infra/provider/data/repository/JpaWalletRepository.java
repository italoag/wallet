package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.WalletMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaWalletRepository implements WalletRepository {
    private final SpringDataWalletRepository springDataWalletRepository;
    private final WalletMapper walletMapper;

    @Autowired
    public JpaWalletRepository(SpringDataWalletRepository springDataWalletRepository, WalletMapper walletMapper) {
        this.springDataWalletRepository = springDataWalletRepository;
        this.walletMapper = walletMapper;
    }

    @Override
    public Optional<Wallet> findById(UUID id) {
        return springDataWalletRepository.findById(id)
                .map(walletMapper::toDomain);
    }

    @Override
    public Wallet save(Wallet wallet) {
        WalletEntity entity = walletMapper.toEntity(wallet);
        return walletMapper.toDomain(springDataWalletRepository.save(entity));
    }

    @Override
    public void update(Wallet wallet) {
        WalletEntity entity = walletMapper.toEntity(wallet);
        springDataWalletRepository.save(entity);
    }
}
