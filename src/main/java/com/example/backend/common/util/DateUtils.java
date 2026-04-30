package com.example.backend.common.util;

import java.util.Date;

public class DateUtils {

    private DateUtils() {} // evita instancias

    // fecha actual
    public static Date now() {
        return new Date();
    }

    // fecha con expiración en milisegundos
    public static Date expiration(long milliseconds) {
        return new Date(System.currentTimeMillis() + milliseconds);
    }
}