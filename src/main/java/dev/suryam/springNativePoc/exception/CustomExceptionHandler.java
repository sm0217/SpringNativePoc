package dev.suryam.springNativePoc.exception;

import dev.suryam.springNativePoc.exception.CustomHttpException;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class CustomExceptionHandler {

    @ExceptionHandler(CustomHttpException.class)
    public final ResponseEntity<ExceptionMessage> customHttpException(CustomHttpException ex) {
        ExceptionMessage error = new ExceptionMessage("CustomHttpException test", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(CustomCBException.class)
    public final ResponseEntity<ExceptionMessage> customCBException(CustomCBException ex) {
        ExceptionMessage error = new ExceptionMessage("CustomCBException test", ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Data
    @NoArgsConstructor
    public static class ExceptionMessage {
        String message;
        String details;

        public ExceptionMessage(String message, String details) {
            this.message = message;
            this.details = details;
        }
        //Add getters and setters
    }
}
