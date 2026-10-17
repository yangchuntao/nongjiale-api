package com.suzhong.njl.web.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;

import jakarta.validation.constraints.NotNull;

/**
 * 只能手改①④⑤。supplyReady / articlesReady 一旦出现就拒绝，避免运营以为手改已保存。
 */
@JsonIgnoreProperties(ignoreUnknown = false)
public class ChecklistPatchRequest {

    private Boolean destinationReady;
    private Boolean supportCovered;
    private Boolean dirtyDataCleared;
    private boolean destinationReadySpecified;
    private boolean supportCoveredSpecified;
    private boolean dirtyDataClearedSpecified;
    private boolean supplyReadySpecified;
    private boolean articlesReadySpecified;

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

    @JsonSetter("supplyReady")
    public void setSupplyReady(Boolean ignored) {
        this.supplyReadySpecified = true;
    }

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

    public boolean hasManualChange() {
        return specifiedValue(destinationReadySpecified, destinationReady)
                || specifiedValue(supportCoveredSpecified, supportCovered)
                || specifiedValue(dirtyDataClearedSpecified, dirtyDataCleared);
    }

    private boolean specifiedValue(boolean specified, Boolean value) {
        return specified && value != null;
    }
}
