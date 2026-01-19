package dev.bloco.wallet.hub.infra.provider.data.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
@Table(name = "transaction_fees", indexes = {
        @Index(name = "idx_network_id", columnList = "network_id"),
        @Index(name = "idx_timestamp", columnList = "timestamp")
})
public class TransactionFeeEntity {
    @Id
    private UUID id;

    @Column(name = "network_id", nullable = false)
    private UUID networkId;

    @Column(name = "gas_price", nullable = false, precision = 78, scale = 0)
    private BigDecimal gasPrice;

    @Column(name = "priority_fee", precision = 78, scale = 0)
    private BigDecimal priorityFee;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_level", nullable = false)
    private dev.bloco.wallet.hub.domain.model.transaction.FeeLevel feeLevel;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public void setNetworkId(UUID networkId) {
        this.networkId = networkId;
    }

    public BigDecimal getGasPrice() {
        return gasPrice;
    }

    public void setGasPrice(BigDecimal gasPrice) {
        this.gasPrice = gasPrice;
    }

    public BigDecimal getPriorityFee() {
        return priorityFee;
    }

    public void setPriorityFee(BigDecimal priorityFee) {
        this.priorityFee = priorityFee;
    }

    public dev.bloco.wallet.hub.domain.model.transaction.FeeLevel getFeeLevel() {
        return feeLevel;
    }

    public void setFeeLevel(dev.bloco.wallet.hub.domain.model.transaction.FeeLevel feeLevel) {
        this.feeLevel = feeLevel;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

}
