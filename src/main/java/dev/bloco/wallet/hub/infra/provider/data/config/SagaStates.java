package dev.bloco.wallet.hub.infra.provider.data.config;

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
