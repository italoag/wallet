package dev.bloco.wallet.hub.domain.model.transaction;

import dev.bloco.wallet.hub.domain.event.transaction.TransactionCreatedEvent;
import dev.bloco.wallet.hub.domain.event.transaction.TransactionStatusChangedEvent;
import dev.bloco.wallet.hub.domain.model.common.AggregateRoot;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a blockchain transaction.
 * A transaction involves transferring value from one address to another within
 * a specific network.
 * It encapsulates essential details such as transaction hash, sender and
 * recipient addresses, value,
 * gas details, and the transaction's status within the blockchain lifecycle.
 *
 * A transaction starts as PENDING and can be CONFIRMED or FAILED based on the
 * blockchain processing result.
 * This class extends the AggregateRoot, enabling it to track domain events
 * associated with the transaction state changes.
 */
@Getter
public class Transaction extends AggregateRoot {

    private final UUID networkId;
    private final TransactionHash hash;
    private final String fromAddress;
    private final String toAddress;
    private final BigDecimal value;
    private BigDecimal gasPrice;
    private BigDecimal gasLimit;
    private BigDecimal gasUsed;
    private final String data;
    private final Instant timestamp;
    private Long blockNumber;
    private String blockHash;
    private TransactionStatus status;

    /**
     * Creates a new Transaction instance and registers a TransactionCreatedEvent.
     *
     * @param id          the unique identifier of the transaction
     * @param networkId   the network identifier where the transaction occurs
     * @param hash        the hash representing the transaction
     * @param fromAddress the sender's address
     * @param toAddress   the recipient's address
     * @param value       the value being transferred in the transaction
     * @param data        additional data associated with the transaction
     * @return a new instance of the Transaction class
     */
    public static Transaction create(
            UUID id,
            UUID networkId,
            TransactionHash hash,
            String fromAddress,
            String toAddress,
            BigDecimal value,
            String data) {

        Transaction transaction = new Transaction(
                id, networkId, hash, fromAddress, toAddress, value, data, Instant.now());
        transaction.registerEvent(
                new TransactionCreatedEvent(id, networkId, hash.getValue(), fromAddress, toAddress, null));
        return transaction;
    }

    /**
     * Rehydrates a Transaction from persisted state without emitting domain events.
     * Use this when loading an existing transaction from the database.
     */
    public static Transaction rehydrate(
            UUID id,
            UUID networkId,
            TransactionHash hash,
            String fromAddress,
            String toAddress,
            BigDecimal value,
            String data,
            Instant timestamp,
            Long blockNumber,
            String blockHash,
            TransactionStatus status,
            BigDecimal gasPrice,
            BigDecimal gasLimit,
            BigDecimal gasUsed) {
        Transaction tx = new Transaction(id, networkId, hash, fromAddress, toAddress, value, data, timestamp);
        tx.blockNumber = blockNumber;
        tx.blockHash = blockHash;
        tx.status = status;
        tx.gasPrice = gasPrice;
        tx.gasLimit = gasLimit;
        tx.gasUsed = gasUsed;
        return tx;
    }

    /**
     * Constructs a new Transaction instance with the specified parameters.
     *
     * @param id          the unique identifier of the transaction
     * @param networkId   the network identifier where the transaction occurs
     * @param hash        the hash representing the transaction
     * @param fromAddress the sender's address
     * @param toAddress   the recipient's address
     * @param value       the value being transferred in the transaction
     * @param data        additional data associated with the transaction
     * @param timestamp   the timestamp when the transaction was created
     */
    private Transaction(
            UUID id,
            UUID networkId,
            TransactionHash hash,
            String fromAddress,
            String toAddress,
            BigDecimal value,
            String data,
            Instant timestamp) {
        super(id);
        this.networkId = networkId;
        this.hash = hash;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.value = value;
        this.data = data;
        this.timestamp = timestamp;
        this.status = TransactionStatus.PENDING;
    }

    /**
     * Retrieves the hash of the transaction.
     *
     * @return the hash value of the transaction as a String
     */
    public String getHash() {
        return hash.getValue();
    }

    /**
     * Confirms the transaction by updating its status to CONFIRMED and registering
     * a
     * TransactionStatusChangedEvent. Additionally, updates the block number, block
     * hash,
     * and gas used for the transaction.
     *
     * @param blockNumber the block number in which the transaction is included
     * @param blockHash   the hash of the block containing the transaction
     * @param gasUsed     the amount of gas used by the transaction
     */
    public void confirm(long blockNumber, String blockHash, BigDecimal gasUsed) {
        this.blockNumber = blockNumber;
        this.blockHash = blockHash;
        this.gasUsed = gasUsed;

        TransactionStatus oldStatus = this.status;
        this.status = TransactionStatus.CONFIRMED;

        registerEvent(new TransactionStatusChangedEvent(
                getId(), oldStatus, this.status, null, null));
    }

    /**
     * Updates the transaction's status to FAILED and registers a
     * TransactionStatusChangedEvent with the specified reason.
     *
     * @param reason the explanation or justification for the transaction failure
     */
    public void fail(String reason) {
        TransactionStatus oldStatus = this.status;
        this.status = TransactionStatus.FAILED;

        registerEvent(new TransactionStatusChangedEvent(
                getId(), oldStatus, this.status, reason, null));
    }

    /**
     * Updates the gas price and gas limit information for the transaction.
     *
     * @param gasPrice the price per unit of gas to execute the transaction
     * @param gasLimit the maximum amount of gas that can be used for the
     *                 transaction
     */
    public void setGasInfo(BigDecimal gasPrice, BigDecimal gasLimit) {
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
    }

    /**
     * Determines if the transaction has been confirmed.
     *
     * @return true if the transaction status is CONFIRMED; false otherwise
     */
    public boolean isConfirmed() {
        return this.status == TransactionStatus.CONFIRMED;
    }

    /**
     * Determines if the transaction is in a pending state.
     *
     * @return true if the transaction status is PENDING; false otherwise
     */
    public boolean isPending() {
        return this.status == TransactionStatus.PENDING;
    }

    /**
     * Determines if the transaction's status is set to FAILED.
     *
     * @return true if the transaction status is FAILED; false otherwise
     */
    public boolean isFailed() {
        return this.status == TransactionStatus.FAILED;
    }

    public UUID getNetworkId() {
        return networkId;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public String getToAddress() {
        return toAddress;
    }

    public BigDecimal getValue() {
        return value;
    }

    public BigDecimal getGasPrice() {
        return gasPrice;
    }

    public BigDecimal getGasLimit() {
        return gasLimit;
    }

    public BigDecimal getGasUsed() {
        return gasUsed;
    }

    public String getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public String getBlockHash() {
        return blockHash;
    }

    public TransactionStatus getStatus() {
        return status;
    }

}