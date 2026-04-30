package com.ascmoda.inventory.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ReservationNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleReservationNotFound(ReservationNotFoundException ex,
                                                                   HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateInventoryItemException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateInventoryItemException ex,
                                                         HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({
            InvalidStockStateException.class,
            InvalidReservationStateException.class,
            ExternalCatalogValidationException.class,
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
        return build(HttpStatus.CONFLICT, "Inventory item was modified concurrently", request, List.of());
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
        if (!fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }
        return ResponseEntity.status(status).body(problem);
    }

    private String dataIntegrityMessage(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        String normalizedMessage = message == null ? "" : message.toLowerCase();

        if (normalizedMessage.contains("uk_inventory_items_sku")) {
            return "Inventory SKU already exists";
        }
        if (normalizedMessage.contains("uk_inventory_items_product_variant_id")) {
            return "Inventory item already exists for product variant";
        }
        if (normalizedMessage.contains("ck_inventory_items_quantities")) {
            return "Inventory quantities are not in a valid state";
        }
        if (normalizedMessage.contains("uk_stock_reservations_key")) {
            return "Stock reservation key already exists";
        }
        if (normalizedMessage.contains("ux_stock_reservations_reference_active")) {
            return "Active stock reservation already exists for reference";
        }
        if (normalizedMessage.contains("ck_stock_reservations_quantity")) {
            return "Stock reservation quantity is not valid";
        }

        return "Inventory data constraint violation";
    }
}
