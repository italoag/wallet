package dev.bloco.wallet.hub.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a transaction between wallets. The transaction can be of types
 * such as DEPOSIT, WITHDRAWAL, or TRANSFER. Each transaction is uniquely
 * identified by an ID, contains information about the source and destination
 * wallet IDs, the monetary amount involved, the type of the transaction, and
 * the timestamp when the transaction was created.
 */
@Getter
public class Transaction {
    private final UUID id;
    private final UUID fromWalletId;
    private final UUID toWalletId;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final TransactionType type;

  /**
   * Constructs a new Transaction with the specified details.
   *
   * @param fromWalletId the unique identifier of the wallet from which funds are debited,
   *                     or null if the transaction is a DEPOSIT.
   * @param toWalletId   the unique identifier of the wallet to which funds are credited,
   *                     or null if the transaction is a WITHDRAWAL.
   * @param amount       the monetary amount involved in the transaction.
   * @param type         the type of the transaction, which can be DEPOSIT, WITHDRAWAL, or TRANSFER.
   */
    public Transaction(UUID fromWalletId, UUID toWalletId, BigDecimal amount, TransactionType type) {
        this.id = UUID.randomUUID();
        this.fromWalletId = fromWalletId;
        this.toWalletId = toWalletId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.type = type;
    }

  /**
   * Enum representing the different types of transactions that can occur
   * within the wallet system. The transaction types include:
   *<p/>
   * - DEPOSIT: A transaction in which funds are added to a wallet.
   * - WITHDRAWAL: A transaction in which funds are removed from a wallet.
   * - TRANSFER: A transaction in which funds are moved from one wallet to another.
   */
  public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }
}
