package dev.bloco.wallet.hub.infra.provider.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a transaction entity in the database. This class maps to the "transactions" table
 * and provides fields and methods to manage transaction data.
 *<p/>
 * Fields:
 * - id: The unique identifier of the transaction.
 * - fromWalletId: The UUID of the wallet from which the amount is debited.
 * - toWalletId: The UUID of the wallet to which the amount is credited.
 * - amount: The amount involved in the transaction, which is mandatory.
 * - timestamp: The timestamp indicating when the transaction occurred, which is mandatory.
 * - type: The type of transaction being performed (e.g., DEPOSIT, WITHDRAWAL, TRANSFER), which is mandatory.
 *<p/>
 * An internal enum, TransactionType, is defined to represent the type of the transaction.
 */
@Setter
@Getter
@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID fromWalletId;
    private UUID toWalletId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

  /**
   * Represents the type of transaction within the system.
   * This enum defines the allowable types of transactions that
   * can be performed, supporting consistency and validation.
   *<p/>
   * Types:
   * - DEPOSIT: Represents a transaction where funds are added to a wallet.
   * - WITHDRAWAL: Represents a transaction where funds are withdrawn from a wallet.
   * - TRANSFER: Represents a transaction where funds are transferred between two wallets.
   */
  public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }
}
