package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.suzhong.njl.domain.enums.ArticleStatus;

/**
 * 门户攻略文章，对应 {@code article} 表。
 *
 * <p>由运营在后台维护，门户只展示 {@link ArticleStatus#PUBLISHED} 的条目；
 * 已发布文章的直链即使所属城市后来关城仍可打开，但不会进入门户列表和首页推荐。
 *
 * <p>生命周期：草稿（{@code DRAFT}）→ 已发布（{@code PUBLISHED}），首次发布时写入
 * {@code published_at}；退回草稿不会清空该时间。
 */
@TableName("article")
public class Article {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章标题，非空，最长 200 字。 */
    private String title;

    /** 封面图 URL，可空；只收 URL，不做文件上传。 */
    private String coverUrl;

    /** 列表摘要，可空，最长 500 字。 */
    private String summary;

    /** 正文文本，可空；门户按原文本输出，不做富文本清洗。 */
    private String body;

    /** 发布状态，枚举 {@link ArticleStatus}：{@code DRAFT} 草稿、{@code PUBLISHED} 已发布。 */
    private ArticleStatus status;

    /** 归属地区 ID，对应 {@code region.id}，可空；可指向省/市/区县，用于门户按城市过滤。 */
    private Long regionId;

    /** 首次发布时间，可空；仅在状态首次变为 {@code PUBLISHED} 时写入，退回草稿后保留原值。 */
    private LocalDateTime publishedAt;

    /** 创建人 ID，对应 {@code admin_user.id}，非空。 */
    private Long createdBy;

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

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ArticleStatus getStatus() {
        return status;
    }

    public void setStatus(ArticleStatus status) {
        this.status = status;
    }

    public Long getRegionId() {
        return regionId;
    }

    public void setRegionId(Long regionId) {
        this.regionId = regionId;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
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
