package dev.bloco.wallet.hub.domain.exception;

public class InvalidSignatureException extends DomainException {
    
    public InvalidSignatureException(String signatureId) {
        super("INVALID_SIGNATURE", 
              "The signature with ID '" + signatureId + "' is invalid.");
    }
    
}
