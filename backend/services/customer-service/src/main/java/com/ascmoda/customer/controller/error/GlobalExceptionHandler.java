package com.ascmoda.customer.controller.error;

import com.ascmoda.customer.domain.exception.CustomerAddressNotFoundException;
import com.ascmoda.customer.domain.exception.CustomerNotFoundException;
import com.ascmoda.customer.domain.exception.DuplicateEmailException;
import com.ascmoda.customer.domain.exception.DuplicateExternalUserIdException;
import com.ascmoda.customer.domain.exception.IllegalBusinessStateException;
import com.ascmoda.customer.domain.exception.InactiveAddressOperationException;
import com.ascmoda.customer.domain.exception.InvalidCustomerStatusTransitionException;
import com.ascmoda.customer.domain.exception.InvalidDefaultAddressOperationException;
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

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCustomerNotFound(CustomerNotFoundException ex,
                                                                HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(CustomerAddressNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleCustomerAddressNotFound(CustomerAddressNotFoundException ex,
                                                                       HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "CUSTOMER_ADDRESS_NOT_FOUND", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateEmail(DuplicateEmailException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateExternalUserIdException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateExternalUserId(DuplicateExternalUserIdException ex,
                                                                       HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DUPLICATE_EXTERNAL_USER_ID", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
                                                              HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
                "Customer data was modified concurrently", request, List.of());
    }

    @ExceptionHandler({
            InvalidCustomerStatusTransitionException.class,
            InvalidDefaultAddressOperationException.class,
            InactiveAddressOperationException.class,
            IllegalBusinessStateException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ProblemDetail> handleBusinessRule(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex,
                                                          HttpServletRequest request) {
        List<FieldValidationError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::toFieldError)
                .toList();

        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ProblemDetail> handleBadRequest(Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException ex,
                                                             HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION", dataIntegrityMessage(ex), request, List.of());
    }

    private FieldValidationError toFieldError(FieldError error) {
        return new FieldValidationError(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ProblemDetail> build(HttpStatus status, String errorCode, String message,
                                                HttpServletRequest request,
                                                List<FieldValidationError> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", errorCode);
        problem.setProperty("timestamp", Instant.now());
        if (!fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }
        return ResponseEntity.status(status).body(problem);
    }

    private String dataIntegrityMessage(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        String normalizedMessage = message == null ? "" : message.toLowerCase();

        if (normalizedMessage.contains("ux_customers_email")) {
            return "Customer email already exists";
        }
        if (normalizedMessage.contains("ux_customers_external_user_id")) {
            return "Customer external user id already exists";
        }
        if (normalizedMessage.contains("ux_customer_addresses_default_shipping")) {
            return "Customer already has a default shipping address";
        }
        if (normalizedMessage.contains("ux_customer_addresses_default_billing")) {
            return "Customer already has a default billing address";
        }
        if (normalizedMessage.contains("ck_customer_addresses_inactive_not_default")) {
            return "Inactive address cannot be default address";
        }

        return "Customer data constraint violation";
    }
}
