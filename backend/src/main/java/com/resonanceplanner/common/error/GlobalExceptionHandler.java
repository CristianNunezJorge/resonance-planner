package com.resonanceplanner.common.error;

import com.resonanceplanner.common.api.ApiError;
import com.resonanceplanner.common.api.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every exception into an ApiResponse with the right HTTP status (D46).
 * Extending ResponseEntityExceptionHandler covers all the standard Spring MVC errors
 * (malformed JSON, unknown path, wrong method...): createResponseEntity gives them our format.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String MALFORMED_REQUEST_MESSAGE = "Richiesta non valida.";

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return failure(ex.getErrorCode(), ex.getMessage());
    }

    // Login with wrong credentials (401) and denied access (403), when thrown inside a controller
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        ErrorCode code = ex instanceof BadCredentialsException ? ErrorCode.BAD_CREDENTIALS : ErrorCode.UNAUTHORIZED;
        return failure(code, code.defaultMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return failure(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage());
    }

    // Thrown by @Validated controllers and services
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getConstraintViolations()
                .forEach(violation -> fields.putIfAbsent(lastNodeName(violation.getPropertyPath()), violation.getMessage()));
        return validationFailure(fields);
    }

    // Anything we did not foresee: full details in the logs, a generic message to the client
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return failure(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage());
    }

    // Invalid @Valid request body: one message for each invalid field
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return validationFailure(fields);
    }

    // Invalid parameters with constraints such as @Min, possibly together with a @Valid body
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> collectFields(result, fields));
        return validationFailure(fields);
    }

    // Logs server-side errors, then lets Spring run its own checks (for example a response already sent)
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("Spring MVC error", ex);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    // Final step for every standard Spring MVC error: replace Spring's body with ours
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ErrorCode code = codeFor(statusCode);
        String message = code == ErrorCode.VALIDATION_ERROR ? MALFORMED_REQUEST_MESSAGE : code.defaultMessage();
        return ResponseEntity.status(statusCode).headers(headers).body(errorBody(code, message));
    }

    // Statuses without their own code in docs/05-api.md (405, 415...) count as invalid requests
    private static ErrorCode codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 409 -> ErrorCode.CONFLICT;
            case 429 -> ErrorCode.RATE_LIMITED;
            default -> status.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.VALIDATION_ERROR;
        };
    }

    // A @Valid body reports its own fields; a simple parameter is reported by its name
    private static void collectFields(ParameterValidationResult result, Map<String, String> fields) {
        if (result instanceof ParameterErrors errors) {
            errors.getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
            return;
        }
        result.getResolvableErrors().stream()
                .findFirst()
                .ifPresent(error -> fields.putIfAbsent(result.getMethodParameter().getParameterName(), error.getDefaultMessage()));
    }

    private static String lastNodeName(Path path) {
        String name = null;
        for (Path.Node node : path) {
            name = node.getName();
        }
        return name;
    }

    private static ResponseEntity<Object> validationFailure(Map<String, String> fields) {
        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        // Read-only view that keeps the order of the fields
        ApiError error = new ApiError(code.name(), code.defaultMessage(), Collections.unmodifiableMap(fields));
        return ResponseEntity.status(code.status()).body(ApiResponse.failure(error));
    }

    private static ResponseEntity<ApiResponse<Void>> failure(ErrorCode code, String message) {
        return ResponseEntity.status(code.status()).body(errorBody(code, message));
    }

    private static ApiResponse<Void> errorBody(ErrorCode code, String message) {
        return ApiResponse.failure(new ApiError(code.name(), message, null));
    }
}
