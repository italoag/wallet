package dev.bloco.wallet.hub.infra.provider.data.repository;

import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.infra.provider.data.entity.WalletEntity;
import dev.bloco.wallet.hub.infra.provider.mapper.WalletMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("Wallet Repository Tests")
@ExtendWith(MockitoExtension.class)
class JpaWalletRepositoryTest {

    @Mock
    private SpringDataWalletRepository springDataWalletRepository;

    @Mock
    private WalletMapper walletMapper;

    private JpaWalletRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaWalletRepository(springDataWalletRepository, walletMapper);
    }

    @Test
    @DisplayName("Save and find by id")
    void findById_shouldMapEntityToDomain() {
        UUID id = UUID.randomUUID();
        WalletEntity entity = new WalletEntity();
        entity.setId(id);
        entity.setUserId(UUID.randomUUID());
        entity.setBalance(new BigDecimal("10.00"));

        Wallet domain = new Wallet(entity.getUserId());
        domain.addFunds(new BigDecimal("10.00"));

        when(springDataWalletRepository.findById(id)).thenReturn(Optional.of(entity));
        when(walletMapper.toDomain(entity)).thenReturn(domain);

        Optional<Wallet> result = repository.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get()).isSameAs(domain);
        verify(springDataWalletRepository).findById(id);
        verify(walletMapper).toDomain(entity);
    }

    @Test
    @DisplayName("Save should delegate to SpringDataUserRepository")
    void save_shouldMapDomainToEntity_andBackToDomain() {
        UUID userId = UUID.randomUUID();
        Wallet toSave = new Wallet(userId);
        toSave.addFunds(new BigDecimal("5.00"));

        WalletEntity mappedEntity = new WalletEntity();
        mappedEntity.setUserId(userId);
        mappedEntity.setBalance(new BigDecimal("5.00"));

        WalletEntity savedEntity = new WalletEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setUserId(userId);
        savedEntity.setBalance(new BigDecimal("5.00"));

        Wallet mappedBack = new Wallet(userId);
        mappedBack.addFunds(new BigDecimal("5.00"));

        when(walletMapper.toEntity(toSave)).thenReturn(mappedEntity);
        when(springDataWalletRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(walletMapper.toDomain(savedEntity)).thenReturn(mappedBack);

        Wallet result = repository.save(toSave);

        assertThat(result).isSameAs(mappedBack);

        ArgumentCaptor<WalletEntity> captor = ArgumentCaptor.forClass(WalletEntity.class);
        verify(springDataWalletRepository).save(captor.capture());
        WalletEntity captured = captor.getValue();
        assertThat(captured.getUserId()).isEqualTo(userId);
        assertThat(captured.getBalance()).isEqualByComparingTo("5.00");

        verify(walletMapper).toEntity(toSave);
        verify(walletMapper).toDomain(savedEntity);
        verifyNoMoreInteractions(walletMapper, springDataWalletRepository);
    }

    @Test
    @DisplayName("Update should delegate to SpringDataUserRepository")
    void update_shouldMapDomainToEntity_andDelegateToSave() {
        UUID userId = UUID.randomUUID();
        Wallet wallet = new Wallet(userId);
        wallet.addFunds(new BigDecimal("7.50"));

        WalletEntity entity = new WalletEntity();
        entity.setUserId(userId);
        entity.setBalance(new BigDecimal("7.50"));

        when(walletMapper.toEntity(wallet)).thenReturn(entity);
        when(springDataWalletRepository.save(entity)).thenReturn(entity);

        repository.update(wallet);

        verify(walletMapper).toEntity(wallet);
        verify(springDataWalletRepository).save(entity);
        verifyNoMoreInteractions(walletMapper, springDataWalletRepository);
    }
}
