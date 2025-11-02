package net.mooctest;

public class MementoException extends Exception {
    public MementoException(String message) {
        super(message);
    }
    public MementoException(String message, Throwable cause) {
        super(message, cause);
    }
}
