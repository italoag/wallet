package dev.bloco.wallet.hub.domain.exception;

public abstract class DomainException extends RuntimeException {
    
    private final String errorCode;

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
}
