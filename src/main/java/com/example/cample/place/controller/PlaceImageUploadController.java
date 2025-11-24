// src/main/java/com/example/cample/place/controller/PlaceImageUploadController.java
package com.example.cample.place.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/place-images")
public class PlaceImageUploadController {

    @Value("${app.upload.dir:./uploads}")
    private String uploadRoot;

    @Value("${app.public-base-url:}")
    private String publicBaseUrl;

    @Value("${server.servlet.context-path:}")
    private String contextPath;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> upload(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest req
    ) throws IOException {

        // 기본 검증
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "EMPTY_FILE"));
        }
        if (file.getSize() > 10L * 1024 * 1024) { // 10MB
            return ResponseEntity.badRequest().body(Map.of("error", "FILE_TOO_LARGE"));
        }
        String contentType = Optional.ofNullable(file.getContentType()).orElse("");
        if (!contentType.toLowerCase().startsWith("image/")) {
            return ResponseEntity.badRequest().body(Map.of("error", "NOT_IMAGE"));
        }

        // 저장 경로: {app.upload.dir}/place-images/랜덤파일명
        String original = StringUtils.cleanPath(
                Optional.ofNullable(file.getOriginalFilename()).orElse("")
        );
        String ext = "";
        int idx = original.lastIndexOf('.');
        if (idx >= 0) {
            ext = original.substring(idx); // .jpg, .png ...
        }

        String filename = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = Paths.get(uploadRoot, "place-images");
        Files.createDirectories(dir);
        Path target = dir.resolve(filename);
        Files.write(target, file.getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

        // 정적 매핑 기준 상대 경로
        // StaticResourceConfig 에서 /files/** -> {app.upload.dir}/** 로 매핑했으므로
        String relPath = "/files/place-images/" + filename;

        // base URL 결정
        String base;
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            // properties 의 app.public-base-url 그대로 사용
            base = publicBaseUrl.replaceAll("/+$", "");
        } else {
            // 미지정 시, 요청 정보로 조합
            String scheme = req.getScheme();
            String host = req.getServerName();
            int port = req.getServerPort();
            String cp = (contextPath == null) ? "" : contextPath.trim();

            base = scheme + "://" + host
                    + ((port == 80 || port == 443) ? "" : (":" + port))
                    + (cp.startsWith("/") ? cp : (cp.isEmpty() ? "" : "/" + cp));
        }

        String url = (base + relPath).replaceAll("(?<!:)/{2,}", "/");

        return ResponseEntity.ok(Map.of(
                // 여기 url 을 places.image_url 에 저장해서 쓰면 됨
                "url", url,
                "path", relPath,
                "saved", target.toAbsolutePath().toString()
        ));
    }
}
