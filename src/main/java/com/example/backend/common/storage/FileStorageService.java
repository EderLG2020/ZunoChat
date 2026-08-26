package com.example.backend.common.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstracción de almacenamiento de archivos — quién implementa esto se elige
 * por propiedad (app.storage.provider=local|cloudinary, ver application.properties),
 * no en el código que la usa (UploadController). Mismo patrón que
 * IPresenceService/IWebSocketSessionRegistry para memoria vs Redis.
 */
public interface FileStorageService {
    StoredFile upload(MultipartFile file, String folder);
}
