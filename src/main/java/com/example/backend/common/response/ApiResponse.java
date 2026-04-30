package com.example.backend.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Envoltorio unificado para todas las respuestas de la API.
 *
 * Patrón de éxito:
 * {
 *   "success":   true,
 *   "code":      "OK_LOGIN",
 *   "status":    200,
 *   "message":   "Sesión iniciada correctamente",
 *   "timestamp": "2026-04-30T17:01:09.388628",
 *   "data":      { ... }          ← presente solo en éxito
 * }
 *
 * Patrón de error:
 * {
 *   "success":   false,
 *   "code":      "OTP_EXPIRED",
 *   "status":    400,
 *   "message":   "El código OTP ha expirado. Solicita uno nuevo",
 *   "timestamp": "2026-04-30T17:01:09.388628",
 *   "errors":    { ... }          ← presente solo en errores de validación
 * }
 */
@JsonPropertyOrder({"success", "code", "status", "message", "timestamp", "data", "errors"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    private final boolean       success;
    private final String        code;
    private final int           status;
    private final String        message;
    private final String        timestamp;
    private final T             data;
    private final Map<String, String> errors;

    // Constructor completo (privado — usar los factory methods)
    private ApiResponse(boolean success, String code, int status,
                        String message, T data, Map<String, String> errors) {
        this.success   = success;
        this.code      = code;
        this.status    = status;
        this.message   = message;
        this.timestamp = LocalDateTime.now().format(FORMATTER);
        this.data      = data;
        this.errors    = errors;
    }

    // ─── Factory methods de éxito ─────────────────────────────────────────────

    /** Éxito con datos */
    public static <T> ApiResponse<T> ok(AppCode appCode, T data) {
        return new ApiResponse<>(true, appCode.getCode(), appCode.getHttpStatusValue(),
                appCode.getMessage(), data, null);
    }

    /** Éxito con datos y mensaje personalizado */
    public static <T> ApiResponse<T> ok(AppCode appCode, String customMessage, T data) {
        return new ApiResponse<>(true, appCode.getCode(), appCode.getHttpStatusValue(),
                customMessage, data, null);
    }

    /** Éxito sin datos (ej: ban, eliminar) */
    public static <Void> ApiResponse<Void> ok(AppCode appCode) {
        return new ApiResponse<>(true, appCode.getCode(), appCode.getHttpStatusValue(),
                appCode.getMessage(), null, null);
    }

    // ─── Factory methods de error ─────────────────────────────────────────────

    /** Error estándar */
    public static <T> ApiResponse<T> error(AppCode appCode) {
        return new ApiResponse<>(false, appCode.getCode(), appCode.getHttpStatusValue(),
                appCode.getMessage(), null, null);
    }

    /** Error con mensaje personalizado */
    public static <T> ApiResponse<T> error(AppCode appCode, String customMessage) {
        return new ApiResponse<>(false, appCode.getCode(), appCode.getHttpStatusValue(),
                customMessage, null, null);
    }

    /** Error de validación con mapa de campos → mensaje */
    public static <T> ApiResponse<T> validationError(Map<String, String> fieldErrors) {
        return new ApiResponse<>(false, AppCode.VALID_FIELDS.getCode(),
                AppCode.VALID_FIELDS.getHttpStatusValue(),
                AppCode.VALID_FIELDS.getMessage(), null, fieldErrors);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public boolean              isSuccess()   { return success; }
    public String               getCode()     { return code; }
    public int                  getStatus()   { return status; }
    public String               getMessage()  { return message; }
    public String               getTimestamp(){ return timestamp; }
    public T                    getData()     { return data; }
    public Map<String, String>  getErrors()   { return errors; }
}
