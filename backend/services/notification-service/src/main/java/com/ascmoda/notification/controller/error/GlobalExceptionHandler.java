package com.ascmoda.notification.controller.error;

import com.ascmoda.notification.domain.exception.DuplicateEventProcessingException;
import com.ascmoda.notification.domain.exception.InvalidMessagePayloadException;
import com.ascmoda.notification.domain.exception.InvalidNotificationStateException;
import com.ascmoda.notification.domain.exception.NotificationNotFoundException;
import com.ascmoda.notification.domain.exception.NotificationSenderException;
import com.ascmoda.notification.domain.exception.UnsupportedNotificationEventException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotificationNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of(), "NOTIFICATION_NOT_FOUND");
    }

    @ExceptionHandler(DuplicateEventProcessingException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateEventProcessingException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of(), "DUPLICATE_EVENT");
    }

    @ExceptionHandler({
            InvalidMessagePayloadException.class,
            UnsupportedNotificationEventException.class,
            InvalidNotificationStateException.class,
            NotificationSenderException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ProblemDetail> handleBusiness(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, List.of(), "NOTIFICATION_BUSINESS_ERROR");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors, "VALIDATION_ERROR");
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of(), "BAD_REQUEST");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, dataIntegrityMessage(ex), request, List.of(), "DATA_CONSTRAINT_VIOLATION");
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Notification was modified concurrently", request, List.of(), "NOTIFICATION_CONFLICT");
    }

    private FieldValidationError toFieldError(FieldError error) {
        return new FieldValidationError(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String message, HttpServletRequest request,
                                                List<FieldValidationError> fieldErrors, String errorCode) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", errorCode);
        if (!fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }
        return ResponseEntity.status(status).body(problem);
    }

    private String dataIntegrityMessage(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        String normalized = message == null ? "" : message.toLowerCase();
        if (normalized.contains("uk_notification_messages_event_id")) {
            return "Notification event was already processed";
        }
        return "Notification data constraint violation";
    }
}
