package dev.bloco.wallet.hub.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Transaction {
    private final UUID id;
    private final UUID fromWalletId;
    private final UUID toWalletId;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final TransactionType type;

    public Transaction(UUID fromWalletId, UUID toWalletId, BigDecimal amount, TransactionType type) {
        this.id = UUID.randomUUID();
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.type = type;
    }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }
}
