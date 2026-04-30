package com.example.backend.common.exception;

import com.example.backend.common.response.AppCode;

/**
 * Excepción del sistema que transporta un AppCode.
 *
 * Uso en servicios:
 *   throw new AppException(AppCode.OTP_EXPIRED);
 *   throw new AppException(AppCode.USER_NOT_FOUND, "No existe el usuario con id 42");
 *
 * El GlobalExceptionHandler la captura y construye el ApiResponse automáticamente.
 */
public class AppException extends RuntimeException {

    private final AppCode appCode;

    public AppException(AppCode appCode) {
        super(appCode.getMessage());
        this.appCode = appCode;
    }

    public AppException(AppCode appCode, String customMessage) {
        super(customMessage);
        this.appCode = appCode;
    }

    public AppCode getAppCode()      { return appCode; }
    public String  getCustomMessage(){ return getMessage(); }
}
