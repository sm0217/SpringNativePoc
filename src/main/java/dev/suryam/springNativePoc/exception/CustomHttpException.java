package dev.suryam.springNativePoc.exception;

public class CustomHttpException extends RuntimeException {

    public CustomHttpException(String message) {
        super("CustomHttpException");
    }
}
