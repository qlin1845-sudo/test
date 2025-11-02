package net.mooctest;

/**
 * Thrown when input validation fails.
 */
public class ValidationException extends DomainException {
    public ValidationException(String message) {
        super(message);
    }
}


