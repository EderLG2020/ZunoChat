package com.example.backend.common.storage;

/** url = link público para servir el archivo. publicId = identificador interno del proveedor (para poder borrarlo después). */
public record StoredFile(String url, String publicId) {}
