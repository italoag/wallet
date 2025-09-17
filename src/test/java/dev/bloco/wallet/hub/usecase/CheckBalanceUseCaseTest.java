package dev.bloco.wallet.hub.usecase;

import dev.bloco.wallet.hub.domain.Wallet;
import dev.bloco.wallet.hub.domain.gateway.WalletRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Check Balance Use Case Tests")
class CheckBalanceUseCaseTest {

    @Test
    @DisplayName("checkBalance returns wallet balance when wallet exists")
    void checkBalance_success() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        CheckBalanceUseCase useCase = new CheckBalanceUseCase(walletRepository);

        UUID walletId = UUID.randomUUID();
        Wallet wallet = new Wallet(UUID.randomUUID());
        wallet.addFunds(new BigDecimal("123.45"));
        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));

        BigDecimal balance = useCase.checkBalance(walletId);

        assertThat(balance).isEqualByComparingTo(new BigDecimal("123.45"));
        verify(walletRepository).findById(walletId);
        verifyNoMoreInteractions(walletRepository);
    }

    @Test
    @DisplayName("checkBalance throws when wallet not found")
    void checkBalance_walletNotFound() {
        WalletRepository walletRepository = mock(WalletRepository.class);
        CheckBalanceUseCase useCase = new CheckBalanceUseCase(walletRepository);

        UUID walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.checkBalance(walletId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wallet not found");

        verify(walletRepository).findById(walletId);
        verifyNoMoreInteractions(walletRepository);
    }
}
