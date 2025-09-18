package dev.bloco.wallet.hub.infra.provider.data.config;

/**
 * Enumeration representing the events that can occur during a saga process.
 *<p/>
 * A saga is a coordination pattern used to manage distributed transactions
 * across multiple services. This enumeration defines the events that trigger
 * transitions between different saga states in the state machine.
 *<p/>
 * The following events are supported:
 * - WALLET_CREATED: Indicates the creation of a wallet in the saga process.
 * - FUNDS_ADDED: Represents the addition of funds to a wallet during the saga.
 * - FUNDS_WITHDRAWN: Represents the withdrawal of funds from a wallet in the saga.
 * - FUNDS_TRANSFERRED: Denotes the transfer of funds between wallets within the saga.
 * - SAGA_COMPLETED: Indicates the successful completion of the saga process.
 * - SAGA_FAILED: Indicates a failure in the saga process.
 *<p/>
 * These events are used within the state machine to determine transitions
 * between states, as defined in the saga workflow configuration.
 */
public enum SagaEvents {
    WALLET_CREATED,
    FUNDS_ADDED,
    FUNDS_WITHDRAWN,
    FUNDS_TRANSFERRED,
    SAGA_COMPLETED,
    SAGA_FAILED
}
