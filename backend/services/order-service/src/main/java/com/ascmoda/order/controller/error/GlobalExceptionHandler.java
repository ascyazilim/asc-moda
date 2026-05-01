package com.ascmoda.order.controller.error;

import com.ascmoda.order.domain.exception.CartNotReadyException;
import com.ascmoda.order.domain.exception.CheckoutPreviewInvalidException;
import com.ascmoda.order.domain.exception.DuplicateOrderAttemptException;
import com.ascmoda.order.domain.exception.EmptyCheckoutSelectionException;
import com.ascmoda.order.domain.exception.ExternalServiceUnavailableException;
import com.ascmoda.order.domain.exception.InventoryConsumeFailedException;
import com.ascmoda.order.domain.exception.InventoryReleaseFailedException;
import com.ascmoda.order.domain.exception.InventoryReservationFailedException;
import com.ascmoda.order.domain.exception.InvalidOrderStateException;
import com.ascmoda.order.domain.exception.OrderNotFoundException;
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

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(OrderNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateOrderAttemptException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateOrderAttemptException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({
            CartNotReadyException.class,
            CheckoutPreviewInvalidException.class,
            EmptyCheckoutSelectionException.class,
            InventoryConsumeFailedException.class,
            InventoryReleaseFailedException.class,
            InventoryReservationFailedException.class,
            InvalidOrderStateException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ProblemDetail> handleBusinessRule(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ExternalServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> handleExternalServiceUnavailable(ExternalServiceUnavailableException ex,
                                                                          HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, dataIntegrityMessage(ex), request, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Order was modified concurrently", request, List.of());
    }

    private FieldValidationError toFieldError(FieldError error) {
        return new FieldValidationError(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String message, HttpServletRequest request,
                                                List<FieldValidationError> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("errorCode", errorCode(message, status));
        if (!fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }
        return ResponseEntity.status(status).body(problem);
    }

    private String errorCode(String message, HttpStatus status) {
        String normalized = message == null ? "" : message.toLowerCase();
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return "EXTERNAL_SERVICE_UNAVAILABLE";
        }
        if (normalized.contains("not found")) {
            return "ORDER_NOT_FOUND";
        }
        if (normalized.contains("idempotency") || normalized.contains("already exists")) {
            return "DUPLICATE_ORDER_ATTEMPT";
        }
        if (normalized.contains("checkout")) {
            return "CHECKOUT_NOT_READY";
        }
        if (normalized.contains("reservation")) {
            return "INVENTORY_RESERVATION_ERROR";
        }
        if (normalized.contains("concurrently")) {
            return "ORDER_CONFLICT";
        }
        if (status == HttpStatus.BAD_REQUEST) {
            return "VALIDATION_ERROR";
        }
        return "ORDER_BUSINESS_ERROR";
    }

    private String dataIntegrityMessage(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        String normalizedMessage = message == null ? "" : message.toLowerCase();

        if (normalizedMessage.contains("uk_orders_order_number")) {
            return "Order number already exists";
        }
        if (normalizedMessage.contains("uk_orders_source_cart_id")) {
            return "Order already exists for cart";
        }
        if (normalizedMessage.contains("ux_orders_customer_idempotency")) {
            return "Order already exists for idempotency key";
        }
        if (normalizedMessage.contains("uk_orders_external_reference")) {
            return "Order external reference already exists";
        }
        if (normalizedMessage.contains("ck_orders_amounts")) {
            return "Order amounts are not consistent";
        }
        if (normalizedMessage.contains("ck_order_items_prices")) {
            return "Order item totals are not consistent";
        }

        return "Order data constraint violation";
    }
}
