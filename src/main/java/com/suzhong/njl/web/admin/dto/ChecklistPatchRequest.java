package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.NotNull;

/**
 * 开城清单局部更新请求体（{@code PATCH /api/admin/cities/{cityId}/checklist}）。
 *
 * <p>只能手改①目的地数据、④客服覆盖、⑤脏数据清理三项；②供给达标、③攻略达标由实时计数推导，
 * 请求中出现 {@code supplyReady} 或 {@code articlesReady} 即返回 400「供给达标与攻略达标按实时计数自动勾选，不能手改」。
 * 三个手改项至少要有一项传了非 null 值，否则返回 400「请指定要更新的项」。
 * 城市不存在返回 404，城市节点停用返回 400「地区不可用」，版本不一致返回 409「数据已被修改」。
 * 未知字段返回 400「请求体格式不正确」。
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ChecklistPatchRequest {

    /** ①目的地数据是否齐备，可空；null 表示本次不改，非 null 时覆盖原值。 */
    private Boolean destinationReady;
    /** ④客服覆盖是否已说明，可空；null 表示本次不改。 */
    private Boolean supportCovered;
    /** ⑤测数或脏数据是否已确认清理，可空；null 表示本次不改。 */
    private Boolean dirtyDataCleared;
    /** 请求是否显式出现 {@code destinationReady}，用于区分「未传」与「传了 null」。 */
    private boolean destinationReadySpecified;
    /** 请求是否显式出现 {@code supportCovered}。 */
    private boolean supportCoveredSpecified;
    /** 请求是否显式出现 {@code dirtyDataCleared}。 */
    private boolean dirtyDataClearedSpecified;
    /** 请求是否出现只读字段 {@code supplyReady}，出现即拒绝整次更新。 */
    private boolean supplyReadySpecified;
    /** 请求是否出现只读字段 {@code articlesReady}，出现即拒绝整次更新。 */
    private boolean articlesReadySpecified;

    /** 乐观锁版本号，必填；没有清单行时必须是 0，否则 409。 */
    @NotNull(message = "版本不能为空")
    private Long version;

    public void setDestinationReady(Boolean destinationReady) {
        this.destinationReady = destinationReady;
        this.destinationReadySpecified = true;
    }

    public void setSupportCovered(Boolean supportCovered) {
        this.supportCovered = supportCovered;
        this.supportCoveredSpecified = true;
    }

    public void setDirtyDataCleared(Boolean dirtyDataCleared) {
        this.dirtyDataCleared = dirtyDataCleared;
        this.dirtyDataClearedSpecified = true;
    }

    /** 只登记字段出现过，值本身丢弃；由服务层据此返回 400。 */
    @JsonSetter("supplyReady")
    public void setSupplyReady(Boolean ignored) {
        this.supplyReadySpecified = true;
    }

    /** 只登记字段出现过，值本身丢弃；由服务层据此返回 400。 */
    @JsonSetter("articlesReady")
    public void setArticlesReady(Boolean ignored) {
        this.articlesReadySpecified = true;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Boolean getDestinationReady() {
        return destinationReady;
    }

    public Boolean getSupportCovered() {
        return supportCovered;
    }

    public Boolean getDirtyDataCleared() {
        return dirtyDataCleared;
    }

    public Long getVersion() {
        return version;
    }

    public boolean destinationReadySpecified() {
        return destinationReadySpecified;
    }

    public boolean supportCoveredSpecified() {
        return supportCoveredSpecified;
    }

    public boolean dirtyDataClearedSpecified() {
        return dirtyDataClearedSpecified;
    }

    public boolean supplyReadySpecified() {
        return supplyReadySpecified;
    }

    public boolean articlesReadySpecified() {
        return articlesReadySpecified;
    }

    /** 是否至少有一项手改项传了非 null 值；全为「未传」或「传 null」时服务层返回 400。 */
    public boolean hasManualChange() {
        return specifiedValue(destinationReadySpecified, destinationReady)
                || specifiedValue(supportCoveredSpecified, supportCovered)
                || specifiedValue(dirtyDataClearedSpecified, dirtyDataCleared);
    }

    private boolean specifiedValue(boolean specified, Boolean value) {
        return specified && value != null;
    }
}
