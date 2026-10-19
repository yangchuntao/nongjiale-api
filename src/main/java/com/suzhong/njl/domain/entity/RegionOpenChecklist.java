package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;

/**
 * 市的开城手勾清单，对应 {@code region_open_checklist} 表。
 *
 * <p>每个市一行，由运营在开城流程中维护。只存①④⑤三项人工确认；②供给达标和
 * ③攻略达标按实时计数推导，不落库，也不因计数不足自动把城市改回 {@code CLOSED}。
 *
 * <p>开城动作要求全部检查项达标，未达标返回 409；关城只改 {@code region.open_status}，
 * 不删除清单和历史数据。
 */
@TableName("region_open_checklist")
public class RegionOpenChecklist {

    /** 市 ID，对应 {@code region.id}，主键由外部指定（非自增），只允许市一级（level=2）。 */
    @TableId(value = "city_id", type = IdType.INPUT)
    private Long cityId;

    /** ①目的地是否就绪，非空，默认 {@code false}；人工勾选。 */
    private Boolean destinationReady;

    /** ④服务保障是否覆盖，非空，默认 {@code false}；人工勾选。 */
    private Boolean supportCovered;

    /** ⑤脏数据是否清理，非空，默认 {@code false}；人工勾选。 */
    private Boolean dirtyDataCleared;

    /** 乐观锁版本号，默认 0；运营修改清单时必须回传，冲突返回 409。 */
    @Version
    private Long version;

    /** 最后更新时间，插入和更新时自动填充。 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 最后修改人 ID，对应 {@code admin_user.id}，可空；种子数据的初始记录为空。 */
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
