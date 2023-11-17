package dev.suryam.springNativePoc.exception;

public class CustomCBException extends RuntimeException {

    public CustomCBException(String message) {
        super("Circuit breaker in place");
    }
}
