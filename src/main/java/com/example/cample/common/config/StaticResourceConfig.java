// src/main/java/com/example/cample/common/config/StaticResourceConfig.java
package com.example.cample.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        String root = Paths.get(uploadDir)
                .toAbsolutePath()
                .toString()
                .replace("\\", "/");
        if (!root.endsWith("/")) root = root + "/";

        // 실제 디스크:  {app.upload.dir}/.../*
        // URL:         /files/** 또는 /api/files/**
        registry.addResourceHandler("/files/**", "/api/files/**")
                .addResourceLocations("file:" + root)
                .setCachePeriod(31536000); // 캐시 1년(원하면 변경)
    }
}
