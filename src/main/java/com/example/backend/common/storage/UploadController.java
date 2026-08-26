package com.example.backend.common.storage;

import com.example.backend.common.exception.AppException;
import com.example.backend.common.response.ApiResponse;
import com.example.backend.common.response.AppCode;
import com.example.backend.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * POST /api/uploads — sube hasta 3 archivos (imágenes u otros) y devuelve
 * sus URLs, listas para mandar en SendMessageRequest#fileUrls. El proveedor
 * real (disco local vs Cloudinary) lo decide FileStorageService según
 * app.storage.provider — este controller no sabe cuál está activo.
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @Value("${app.storage.max-file-size-mb}")
    private long maxFileSizeMb;

    @Value("${app.storage.max-files-per-request}")
    private int maxFilesPerRequest;

    @PostMapping
    public ResponseEntity<ApiResponse<UploadResponse>> upload(@RequestParam("files") List<MultipartFile> files) {
        if (files.isEmpty() || files.stream().allMatch(MultipartFile::isEmpty))
            throw new AppException(AppCode.UPLOAD_FILE_EMPTY);
        if (files.size() > maxFilesPerRequest)
            throw new AppException(AppCode.UPLOAD_TOO_MANY_FILES);

        long maxBytes = maxFileSizeMb * 1024 * 1024;
        for (MultipartFile file : files) {
            if (file.getSize() > maxBytes)
                throw new AppException(AppCode.UPLOAD_FILE_TOO_LARGE, "El archivo supera el máximo de " + maxFileSizeMb + "MB");
        }

        // Carpeta por usuario — solo para organización, no es control de acceso
        // (las URLs devueltas son públicas, igual que cualquier CDN de imágenes de chat).
        String folder = String.valueOf(JwtUtil.currentUserId());
        List<String> urls = files.stream()
                .map(file -> fileStorageService.upload(file, folder).url())
                .toList();

        return ResponseEntity
                .status(AppCode.OK_FILES_UPLOADED.getHttpStatus())
                .body(ApiResponse.ok(AppCode.OK_FILES_UPLOADED, new UploadResponse(urls)));
    }
}
