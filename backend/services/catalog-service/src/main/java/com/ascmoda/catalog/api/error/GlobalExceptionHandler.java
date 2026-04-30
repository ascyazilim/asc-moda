package com.ascmoda.catalog.api.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
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

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request, List.of());
    }

    @ExceptionHandler({
            BusinessRuleViolationException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ProblemDetail> handleBusinessRule(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request, List.of());
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

        if (normalizedMessage.contains("uk_categories_slug")) {
            return "Category slug already exists";
        }
        if (normalizedMessage.contains("uk_products_slug")) {
            return "Product slug already exists";
        }
        if (normalizedMessage.contains("uk_product_variants_sku")) {
            return "Product variant SKU already exists";
        }
        if (normalizedMessage.contains("ux_product_images_main")) {
            return "A product can have only one active main image";
        }

        return "Catalog data constraint violation";
    }
}
