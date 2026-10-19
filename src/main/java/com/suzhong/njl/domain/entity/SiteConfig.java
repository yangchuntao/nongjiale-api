package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 站点配置键值对，对应 {@code site_config} 表。
 *
 * <p>保存品牌文案和开城阈值，由运营在后台修改；门户 {@code GET /api/config/public}
 * 只读取展示类键，开城阈值只参与运营开城判断。读多写少，结果进 Redis 缓存，
 * 后台更新后由服务主动失效。
 *
 * <p>键的取值见 {@link com.suzhong.njl.domain.enums.SiteConfigKey}；新增键必须同步枚举，
 * 不要在各处硬编码字符串。
 */
@TableName("site_config")
public class SiteConfig {

    /** 配置键，主键，由外部指定（非自增）；取值如 {@code site.name}、{@code site.brand_color}、{@code open_city.min_stores}。 */
    @TableId(value = "config_key", type = IdType.INPUT)
    private String configKey;

    /** 配置值，非空，最长 2000 字；统一按字符串存储，数值型键由读取方自行转换。 */
    private String configValue;

    /** 最后更新时间，插入和更新时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 最后修改人 ID，对应 {@code admin_user.id}，可空；种子数据的初始记录为空。 */
    private Long updatedBy;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

}
