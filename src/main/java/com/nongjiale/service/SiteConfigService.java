package com.nongjiale.service;

import com.nongjiale.domain.SiteConfig;
import com.nongjiale.repository.SiteConfigRepository;
import com.nongjiale.web.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SiteConfigService {
    public static final long SINGLETON_ID = 1L;
    private static final java.util.regex.Pattern COLOR = java.util.regex.Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private final SiteConfigRepository configs;

    @Value("${nongjiale.contact-phone}")
    private String defaultPhone;

    public SiteConfigService(SiteConfigRepository configs) {
        this.configs = configs;
    }

    @Transactional
    public SiteConfig current() {
        return configs.findById(SINGLETON_ID).orElseGet(() -> {
            SiteConfig created = new SiteConfig();
            created.setId(SINGLETON_ID);
            created.setSiteName("");
            created.setPrimaryColor("");
            created.setContactPhone(defaultPhone == null ? "" : defaultPhone);
            return configs.save(created);
        });
    }

    public String contactPhone() {
        String phone = current().getContactPhone();
        if (phone == null || phone.isBlank()) {
            return defaultPhone == null ? "" : defaultPhone;
        }
        return phone;
    }

    public Map<String, String> publicView() {
        SiteConfig c = current();
        Map<String, String> data = new LinkedHashMap<>();
        data.put("siteName", c.getSiteName() == null ? "" : c.getSiteName());
        data.put("primaryColor", c.getPrimaryColor() == null ? "" : c.getPrimaryColor());
        data.put("contactPhone", c.getContactPhone() == null ? "" : c.getContactPhone());
        return data;
    }

    @Transactional
    public Map<String, String> update(String siteName, String primaryColor, String contactPhone) {
        String color = primaryColor == null ? "" : primaryColor.trim();
        if (!color.isEmpty() && !COLOR.matcher(color).matches()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, 400, "主色须为 #RRGGBB");
        }
        SiteConfig c = current();
        c.setSiteName(siteName == null ? "" : siteName);
        c.setPrimaryColor(color);
        c.setContactPhone(contactPhone == null ? "" : contactPhone.trim());
        configs.save(c);
        return publicView();
    }
}
