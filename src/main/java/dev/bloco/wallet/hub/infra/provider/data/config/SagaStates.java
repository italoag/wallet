package dev.bloco.wallet.hub.infra.provider.data.config;

/**
 * Enumeration representing the different states in a saga process.
 *<p/>
 * A saga process is a distributed transaction management pattern,
 * where the saga transitions across multiple states based on events.
 * This enumeration defines all the states that can occur during the
 * lifecycle of a saga.
 *<p/>
 * The following states are defined:
 * - INITIAL: The starting state of the saga.
 * - WALLET_CREATED: The state after a wallet is successfully created.
 * - FUNDS_ADDED: The state after funds are added to the wallet.
 * - FUNDS_WITHDRAWN: The state after funds are withdrawn from the wallet.
 * - FUNDS_TRANSFERRED: The state after funds are transferred between wallets.
 * - COMPLETED: The final state, indicating the saga has been successfully completed.
 * - FAILED: The final state, indicating the saga has failed.
 * - ANY: A generic state representing any state, typically used for failure handling.
 *<p/>
 * These states are used within the context of a state machine,
 * where events trigger transitions from one state to another as
 * part of the saga's workflow.
 */
public enum SagaStates {
    INITIAL,
    WALLET_CREATED,
    FUNDS_ADDED,
    FUNDS_WITHDRAWN,
    FUNDS_TRANSFERRED,
    COMPLETED,
    FAILED,
    ANY
}
