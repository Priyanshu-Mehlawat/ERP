package edu.univ.erp.auth;

/**
 * Exception thrown when a user attempts an operation they don't have permission for.
 */
public class PermissionException extends RuntimeException {
    
    public PermissionException(String message) {
        super(message);
    }
    
    public PermissionException(String message, Throwable cause) {
        super(message, cause);
    }
}
