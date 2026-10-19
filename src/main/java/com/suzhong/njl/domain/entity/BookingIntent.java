package com.suzhong.njl.domain.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.Version;
import com.suzhong.njl.domain.enums.IntentStatus;

/**
 * 预订意向，对应 {@code booking_intent} 表。
 *
 * <p>游客在门户提交、运营在后台跟进，商家端只读。一期没有支付、锁房和佣金，
 * 因此本表只记录联系诉求，不代表已成交订单。
 *
 * <p>状态机见 {@link IntentStatus}：{@code PENDING} 可转 {@code CONFIRMED}、
 * {@code RESCHEDULED}、{@code CLOSED}；{@code CONFIRMED} 可转 {@code CHECKED_IN}、
 * {@code CLOSED}；{@code RESCHEDULED} 可转 {@code CONFIRMED}、{@code CLOSED}；
 * {@code CHECKED_IN} 与 {@code CLOSED} 为终态。并发更新依赖 {@code version} 乐观锁。
 */
@TableName("booking_intent")
public class BookingIntent {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 意向编号，全局唯一，格式为 {@code NJL} + {@code yyyyMMdd} + 8 位大写十六进制随机串；冲突时最多重试 3 次。 */
    private String intentNo;

    /** 提交游客 ID，对应 {@code portal_user.id}，非空；只能查看自己的意向。 */
    private Long userId;

    /** 意向门店 ID，对应 {@code store.id}，非空。 */
    private Long storeId;

    /** 门店归属商家主体 ID，对应 {@code merchant_org.id}，非空；由门店冗余而来，便于商家端按主体过滤。 */
    private Long merchantOrgId;

    /** 到店日期，非空；不得早于当前业务日期（以应用时钟为准）。 */
    private LocalDate checkIn;

    /** 离店日期，可空；与 {@code nights} 至少填写一项。 */
    private LocalDate checkOut;

    /** 入住晚数，可空；{@code checkOut} 为空时必填，取值从 1 开始。 */
    private Integer nights;

    /** 入住人数，非空，最小 1。 */
    private Integer guests;

    /** 联系人姓名，非空，最长 64 字。 */
    private String contactName;

    /** 联系人手机号，非空，须匹配 {@code ^1\d{10}$}；运营看完整号码，商家端列表做中间四位脱敏。 */
    private String contactPhone;

    /** 游客备注，可空，最长 500 字。 */
    private String userRemark;

    /** 意向状态，枚举 {@link IntentStatus}；创建时固定为 {@code PENDING}。 */
    private IntentStatus status;

    /** 乐观锁版本号，创建时为 0；每次状态变更必须回传，冲突时返回 409。 */
    @Version
    private Long version;

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

    public String getIntentNo() {
        return intentNo;
    }

    public void setIntentNo(String intentNo) {
        this.intentNo = intentNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getMerchantOrgId() {
        return merchantOrgId;
    }

    public void setMerchantOrgId(Long merchantOrgId) {
        this.merchantOrgId = merchantOrgId;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public Integer getNights() {
        return nights;
    }

    public void setNights(Integer nights) {
        this.nights = nights;
    }

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getUserRemark() {
        return userRemark;
    }

    public void setUserRemark(String userRemark) {
        this.userRemark = userRemark;
    }

    public IntentStatus getStatus() {
        return status;
    }

    public void setStatus(IntentStatus status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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
