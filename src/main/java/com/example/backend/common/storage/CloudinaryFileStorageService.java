package com.example.backend.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Sube los archivos a Cloudinary — se activa con app.storage.provider=cloudinary
 * y requiere CLOUDINARY_CLOUD_NAME / CLOUDINARY_API_KEY / CLOUDINARY_API_SECRET
 * en el entorno (ver .env.example). A diferencia del proveedor local, los
 * archivos sobreviven redeploys y se comparten entre instancias — pensado
 * para producción o cualquier despliegue con más de un nodo.
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "cloudinary")
public class CloudinaryFileStorageService implements FileStorageService {

    private final Cloudinary cloudinary;

    public CloudinaryFileStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret,
                "secure", true
        ));
    }

    @Override
    @SuppressWarnings("unchecked")
    public StoredFile upload(MultipartFile file, String folder) {
        try {
            Map<String, Object> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "zunochat/" + folder,
                    "resource_type", "auto" // detecta imagen vs archivo genérico solo
            ));
            String url = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            return new StoredFile(url, publicId);
        } catch (IOException e) {
            log.error("[CloudinaryFileStorageService] Error subiendo archivo: {}", e.getMessage());
            throw new AppException(AppCode.SYS_INTERNAL_ERROR, "No se pudo subir el archivo");
        }
    }
}
