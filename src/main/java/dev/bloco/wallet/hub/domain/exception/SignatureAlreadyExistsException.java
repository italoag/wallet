package dev.bloco.wallet.hub.domain.exception;

public class SignatureAlreadyExistsException extends DomainException {
    
    public SignatureAlreadyExistsException(String documentId) {
        super("SIGNATURE_ALREADY_EXISTS", 
              "A signature with ID '" + documentId + "' already exists.");
    }
}
