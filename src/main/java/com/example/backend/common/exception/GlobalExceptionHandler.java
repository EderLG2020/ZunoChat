package com.example.backend.common.exception;

import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Interceptor global de excepciones.
 * Convierte cualquier error en un ApiResponse con el AppCode correspondiente.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Errores de validación de campos (@Valid) → VALID_FIELDS */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity
                .status(AppCode.VALID_FIELDS.getHttpStatus())
                .body(ApiResponse.validationError(fieldErrors));
    }

    /**
     * Errores de validación en @RequestParam/@PathVariable (ej: size > @Max) →
     * VALID_FIELDS. Distinta de MethodArgumentNotValidException, que solo
     * cubre @Valid sobre @RequestBody.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String path = v.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.put(field, v.getMessage());
        }
        return ResponseEntity
                .status(AppCode.VALID_FIELDS.getHttpStatus())
                .body(ApiResponse.validationError(fieldErrors));
    }

    /** Excepciones de negocio propias → el AppCode que traiga */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<?>> handleAppException(AppException ex) {
        return ResponseEntity
                .status(ex.getAppCode().getHttpStatus())
                .body(ApiResponse.error(ex.getAppCode(), ex.getCustomMessage()));
    }

    /** Spring Security: acceso denegado (403) → AUTH_FORBIDDEN */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(AppCode.AUTH_FORBIDDEN.getHttpStatus())
                .body(ApiResponse.error(AppCode.AUTH_FORBIDDEN));
    }

    /** Cualquier otra excepción no controlada → SYS_INTERNAL_ERROR */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGeneric(Exception ex) {
        return ResponseEntity
                .status(AppCode.SYS_INTERNAL_ERROR.getHttpStatus())
                .body(ApiResponse.error(AppCode.SYS_INTERNAL_ERROR));
    }
}
