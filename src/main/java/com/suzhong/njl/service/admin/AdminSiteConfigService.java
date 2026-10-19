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

/**
 * 站点配置服务（运营端，{@code /api/admin/site-config}）。
 *
 * <p>职责：读写门户展示用的站名、主色、客服电话三项配置。可写键白名单固定为
 * {@link SiteConfigKey#NAME}、{@link SiteConfigKey#BRAND_COLOR}、{@link SiteConfigKey#CONTACT_PHONE}；
 * 开城门槛 {@code OPEN_CITY_MIN_STORES}、{@code OPEN_CITY_MIN_ARTICLES} 不在本接口内，
 * 只能通过数据变更维护，避免运营误改门槛。
 *
 * <p>事务边界：{@link #update} 在一个事务内完成三次 upsert 与审计；{@link #get} 只读，不开事务。
 * 写入后失效 {@code site-config:public} 缓存，门户下一次请求回源。
 *
 * <p>字段长度、颜色格式等校验由请求 DTO 完成，本类只负责落库、审计与缓存失效。
 */
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

    /**
     * 读取当前站点配置。
     *
     * @return 站名、主色、客服电话；配置缺行或值为 null 时返回空串，不返回 null
     */
    public SiteConfigResponse get() {
        return new SiteConfigResponse(
                value(SiteConfigKey.NAME),
                value(SiteConfigKey.BRAND_COLOR),
                value(SiteConfigKey.CONTACT_PHONE));
    }

    /**
     * 更新站点配置。
     *
     * <p>流程：三个白名单键分别 upsert（去首尾空白）→ 写 {@code SITE_CONFIG_UPDATE} 审计
     * → 失效门户站点配置缓存 → 回读返回。三项每次都会写入，未改动的值也会覆盖为相同内容。
     *
     * @param adminId 当前运营账号 ID，写入配置 updated_by 与审计
     * @param request 站名、主色、客服电话，均必填，校验由 DTO 完成
     * @return 更新后的三项配置
     */
    @Transactional
    public SiteConfigResponse update(long adminId, SiteConfigWriteRequest request) {
        // 只 upsert 这三个白名单键；开城阈值键不经过本服务，接口也无法传入
        upsert(SiteConfigKey.NAME, request.siteName().trim(), adminId);
        upsert(SiteConfigKey.BRAND_COLOR, request.brandColor().trim(), adminId);
        upsert(SiteConfigKey.CONTACT_PHONE, request.contactPhone().trim(), adminId);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("siteName", request.siteName().trim());
        detail.put("brandColor", request.brandColor().trim());
        detail.put("contactPhone", request.contactPhone().trim());
        auditRecorder.record(adminId, AuditAction.SITE_CONFIG_UPDATE, AuditTarget.SITE_CONFIG, null, detail);
        // 缓存失效失败只记日志，不回滚已写入的配置
        adminCache.evictPublicConfig();
        return get();
    }

    /** 按配置键 upsert：缺行插入，有行覆盖值并记录最后修改人。 */
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

    /** 读配置值，缺行或空值返回空串，避免响应出现 null。 */
    private String value(SiteConfigKey key) {
        SiteConfig row = siteConfigMapper.selectById(key.code());
        return row == null || row.getConfigValue() == null ? "" : row.getConfigValue();
    }
}
