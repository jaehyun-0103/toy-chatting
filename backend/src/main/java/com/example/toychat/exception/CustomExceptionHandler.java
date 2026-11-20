package com.example.toychat.exception;

import com.example.toychat.security.StompHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class CustomExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomExceptionHandler.class);

    private final StompHandler stompHandler;

    public CustomExceptionHandler(StompHandler stompHandler) {
        this.stompHandler = stompHandler;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(
                error -> errors.putIfAbsent(
                        error.getField(),
                        error.getDefaultMessage()
                )
        );

        return ResponseEntity.badRequest().body(
                Map.of(
                        "message", "Invalid request",
                        "errors", errors
                )
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleInvalidJson(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid JSON request"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleInvalidParameter(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Invalid query parameter", "parameter", ex.getName()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("status", ex.getStatusCode().toString());
        response.put("message", ex.getReason());

        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleHttpException(Exception ex) {
        if (ex instanceof ErrorResponse error) {
            HttpStatus status = HttpStatus.resolve(error.getStatusCode().value());

            String message = status == null ? "REQUEST_FAILED" : status.name();

            if (error.getStatusCode().is5xxServerError()) {
                message = "INTERNAL_SERVER_ERROR";

                logger.error("HTTP request failed; exceptionType={}", ex.getClass().getName());
            }

            return ResponseEntity.status(error.getStatusCode())
                    .headers(error.getHeaders())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("message", message));
        }
        logger.error("HTTP request failed; exceptionType={}", ex.getClass().getName());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "INTERNAL_SERVER_ERROR"));
    }

    @MessageExceptionHandler(Exception.class)
    public void handleWebSocketException(Exception ex, SimpMessageHeaderAccessor headers) {
        boolean invalidMessage = isInvalidMessage(ex);

        if (!invalidMessage) {
            logger.error("STOMP message failed; exceptionType={}", ex.getClass().getName());
        }

        stompHandler.rejectSession(headers.getSessionId(), invalidMessage ? "INVALID_MESSAGE" : "MESSAGE_PROCESSING_FAILED");
    }

    private boolean isInvalidMessage(Throwable error) {
        while (error != null) {
            if (error instanceof MessageConversionException || (error instanceof MessagingException && "Invalid content".equals(error.getMessage()))) {
                return true;
            }

            Throwable cause = error.getCause();
            if (cause == error) {
                break;
            }

            error = cause;
        }

        return false;
    }
}