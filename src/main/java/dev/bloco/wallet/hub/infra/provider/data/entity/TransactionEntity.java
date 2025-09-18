package dev.bloco.wallet.hub.infra.provider.data.entity;

import dev.bloco.wallet.hub.domain.model.transaction.TransactionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a blockchain transaction persisted in the database.
 * Aligns with the schema defined in docs/DATABASE.md (TRANSACTION table).
 */
@Setter
@Getter
@Entity
@Table(name = "transactions")
public class TransactionEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID networkId;

    @Column(nullable = false, unique = true)
    private String hash;

    @Column(nullable = false)
    private String fromAddress;

    @Column(nullable = false)
    private String toAddress;

    @Column(nullable = false)
    private BigDecimal value;

    private BigDecimal gasPrice;

    private BigDecimal gasLimit;

    private BigDecimal gasUsed;

    @Lob
    private String data;

    @Column(nullable = false)
    private Instant timestamp;

    private Long blockNumber;

    private String blockHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;
}
