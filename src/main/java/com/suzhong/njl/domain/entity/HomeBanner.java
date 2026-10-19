package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.suzhong.njl.domain.enums.BannerScope;

/**
 * 首页 Banner，对应 {@code home_banner} 表。
 *
 * <p>由运营维护，门户 {@code GET /api/home/banners} 只返回启用条目：全站 Banner 始终在内，
 * 指定市 Banner 仅在该市仍开通时展示；按 {@code sortOrder} 升序，数字越小越靠前。
 *
 * <p>图片只收 URL，不提供上传、买位和曝光统计；启用指定市 Banner 时若投放市已关闭，
 * 后台直接返回 409。
 */
@TableName("home_banner")
public class HomeBanner {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题，可空，最长 64 字；全站改成空标题、或从指定市改成全站时，要把空值写回。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String title;

    /** 图片 URL，非空，最长 512 字；只存 URL，不存二进制。 */
    private String imageUrl;

    /** 点击跳转 URL，非空；可为站内相对路径（如 {@code /?city=yangzhou}）或完整地址。 */
    private String linkUrl;

    /** 投放范围，枚举 {@link BannerScope}：{@code ALL} 全站、{@code CITY} 指定市。 */
    private BannerScope scope;

    /** 投放市 ID，对应 {@code region.id}；{@code scope=ALL} 时为空，{@code scope=CITY} 时为该市 ID。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long cityId;

    /** 展示顺序，非空，升序排列，数字越小越靠前。 */
    private Integer sortOrder;

    /** 是否启用，非空；停用后门户不再返回，但仍保留记录。 */
    private Boolean enabled;

    /** 创建人 ID，对应 {@code admin_user.id}，非空。 */
    private Long createdBy;

    /** 最后修改人 ID，可空；允许显式写回 {@code null}。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long updatedBy;

    /** 创建时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 最后更新时间，插入和更新时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public BannerScope getScope() {
        return scope;
    }

    public void setScope(BannerScope scope) {
        this.scope = scope;
    }

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
