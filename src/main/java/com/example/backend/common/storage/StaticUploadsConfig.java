package com.example.backend.common.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

/**
 * Sirve los archivos guardados por LocalFileStorageService en /uploads/**.
 * Se registra siempre (no solo con app.storage.provider=local) — si el
 * proveedor activo es Cloudinary, esta ruta simplemente no se usa nunca; no
 * hace daño dejarla mapeada.
 */
@Configuration
public class StaticUploadsConfig implements WebMvcConfigurer {

    @Value("${app.storage.local.dir}")
    private String localDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = new File(localDir).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolutePath + File.separator);
    }
}
