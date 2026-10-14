package com.nongjiale.service;

import com.nongjiale.web.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

@Service
public class LicenseMediaService {
    private final Path root;

    public LicenseMediaService(@Value("${nongjiale.upload-dir:data/uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "请上传图片");
        }
        String ext = extension(file.getContentType());
        String name = UUID.randomUUID().toString().replace("-", "") + ext;
        Path dir = root.resolve("licenses");
        try {
            Files.createDirectories(dir);
            Path dest = dir.resolve(name);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest);
            }
        } catch (IOException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "图片保存失败");
        }
        return "/uploads/licenses/" + name;
    }

    private static String extension(String contentType) {
        String ct = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return switch (ct) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "仅支持图片");
        };
    }
}
