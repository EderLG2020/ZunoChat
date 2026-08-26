package com.example.backend.common.storage;

import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.AppCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Guarda los archivos en disco, bajo app.storage.local.dir (por defecto
 * "uploads" relativo al directorio de trabajo del proceso). Los sirve
 * StaticResourceConfig en /uploads/**.
 *
 * Activo por defecto (app.storage.provider=local o sin definir) — no
 * requiere ninguna cuenta ni credencial externa, funciona igual en dev que
 * en un despliegue de un solo nodo. La limitación real: en un despliegue con
 * más de una instancia o con almacenamiento efímero (la mayoría de PaaS), los
 * archivos no sobreviven un reinicio/redeploy ni se comparten entre
 * instancias — para eso está el proveedor "cloudinary".
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.storage", name = "provider", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    @Value("${app.storage.local.dir}")
    private String localDir;

    @Value("${app.storage.local.base-url}")
    private String baseUrl;

    @Override
    public StoredFile upload(MultipartFile file, String folder) {
        try {
            String safeFolder = sanitize(folder);
            Path targetDir = Path.of(localDir, safeFolder);
            Files.createDirectories(targetDir);

            String extension = extractExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path targetPath = targetDir.resolve(filename);

            file.transferTo(targetPath);

            String url = "%s/uploads/%s/%s".formatted(trimTrailingSlash(baseUrl), safeFolder, filename);
            return new StoredFile(url, safeFolder + "/" + filename);
        } catch (IOException e) {
            log.error("[LocalFileStorageService] Error guardando archivo: {}", e.getMessage());
            throw new AppException(AppCode.SYS_INTERNAL_ERROR, "No se pudo guardar el archivo");
        }
    }

    private String sanitize(String value) {
        // Evita path traversal (../) y separadores — solo alfanuméricos, guion y guion bajo.
        return value.replaceAll("[^a-zA-Z0-9_-]", "");
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null) return "";
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) return "";
        return originalFilename.substring(dot).replaceAll("[^a-zA-Z0-9.]", "");
    }

    private String trimTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
