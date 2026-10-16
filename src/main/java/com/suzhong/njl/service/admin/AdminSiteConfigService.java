package com.suzhong.njl.service.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.suzhong.njl.domain.entity.SiteConfig;
import com.suzhong.njl.domain.enums.AuditAction;
import com.suzhong.njl.domain.enums.AuditTarget;
import com.suzhong.njl.domain.enums.SiteConfigKey;
import com.suzhong.njl.mapper.SiteConfigMapper;
import com.suzhong.njl.web.admin.dto.SiteConfigResponse;
import com.suzhong.njl.web.admin.dto.SiteConfigWriteRequest;

@Service
public class AdminSiteConfigService {

    private final SiteConfigMapper siteConfigMapper;
    private final AuditRecorder auditRecorder;
    private final AdminCache adminCache;

    public AdminSiteConfigService(
            SiteConfigMapper siteConfigMapper,
            AuditRecorder auditRecorder,
            AdminCache adminCache) {
        this.siteConfigMapper = siteConfigMapper;
        this.auditRecorder = auditRecorder;
        this.adminCache = adminCache;
    }

    public SiteConfigResponse get() {
        return new SiteConfigResponse(
                value(SiteConfigKey.NAME),
                value(SiteConfigKey.BRAND_COLOR),
                value(SiteConfigKey.CONTACT_PHONE));
    }

    @Transactional
    public SiteConfigResponse update(long adminId, SiteConfigWriteRequest request) {
        upsert(SiteConfigKey.NAME, request.siteName().trim(), adminId);
        upsert(SiteConfigKey.BRAND_COLOR, request.brandColor().trim(), adminId);
        upsert(SiteConfigKey.CONTACT_PHONE, request.contactPhone().trim(), adminId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("siteName", request.siteName().trim());
        detail.put("brandColor", request.brandColor().trim());
        detail.put("contactPhone", request.contactPhone().trim());
        auditRecorder.record(adminId, AuditAction.SITE_CONFIG_UPDATE, AuditTarget.SITE_CONFIG, null, detail);
        adminCache.evictPublicConfig();
        return get();
    }

    private void upsert(SiteConfigKey key, String value, long adminId) {
        SiteConfig row = siteConfigMapper.selectById(key.code());
        if (row == null) {
            row = new SiteConfig();
            row.setConfigKey(key.code());
            row.setConfigValue(value);
            row.setUpdatedBy(adminId);
            siteConfigMapper.insert(row);
            return;
        }
        row.setConfigValue(value);
        row.setUpdatedBy(adminId);
        siteConfigMapper.updateById(row);
    }

    private String value(SiteConfigKey key) {
        SiteConfig row = siteConfigMapper.selectById(key.code());
        return row == null || row.getConfigValue() == null ? "" : row.getConfigValue();
    }
}
