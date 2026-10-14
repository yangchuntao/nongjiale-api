package com.nongjiale.config;

import com.nongjiale.service.LicenseMediaService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LicenseMediaService media;

    public WebConfig(LicenseMediaService media) {
        this.media = media;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = media.root();
        String location = root.toUri().toString();
        if (!location.endsWith("/")) location = location + "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }
}
