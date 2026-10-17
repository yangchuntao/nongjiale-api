package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

/**
 * 市的开城手勾。只存①④⑤。供给达标和攻略达标按实时计数推导，不落库。
 */
@TableName("region_open_checklist")
public class RegionOpenChecklist {

    @TableId(value = "city_id", type = IdType.INPUT)
    private Long cityId;

    private Boolean destinationReady;

    private Boolean supportCovered;

    private Boolean dirtyDataCleared;

    @Version
    private Long version;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    private Long updatedBy;

    public Long getCityId() {
        return cityId;
    }

    public void setCityId(Long cityId) {
        this.cityId = cityId;
    }

    public Boolean getDestinationReady() {
        return destinationReady;
    }

    public void setDestinationReady(Boolean destinationReady) {
        this.destinationReady = destinationReady;
    }

    public Boolean getSupportCovered() {
        return supportCovered;
    }

    public void setSupportCovered(Boolean supportCovered) {
        this.supportCovered = supportCovered;
    }

    public Boolean getDirtyDataCleared() {
        return dirtyDataCleared;
    }

    public void setDirtyDataCleared(Boolean dirtyDataCleared) {
        this.dirtyDataCleared = dirtyDataCleared;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
