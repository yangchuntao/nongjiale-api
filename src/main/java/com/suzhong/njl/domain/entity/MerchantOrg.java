package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;

/**
 * 商家主体，对应 {@code merchant_org} 表。
 *
 * <p>代表一家入驻商户，是门店和商家账号的归属单位；由运营在入驻审核通过时创建，
 * 之后可在后台停用或启用。主体停用后，其下商家账号登录返回 403，旧令牌失效。
 *
 * <p>关键状态：{@code status=DISABLED} 只影响商家端访问，不删除门店和历史数据。
 */
@TableName("merchant_org")
public class MerchantOrg {

    /** 主键，自增；被 {@code merchant_user.merchant_org_id}、{@code store.merchant_org_id} 等引用。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 主体名称，非空，最长 128 字；通常沿用入驻申请的 {@code orgName}。 */
    private String name;

    /** 主体联系人，可空，最长 64 字。 */
    private String contactName;

    /** 主体联系电话，可空，最长 20 字；与登录手机号不强制一致。 */
    private String contactPhone;

    /** 主体状态，枚举 {@link MerchantOrgStatus}：{@code ACTIVE} 启用、{@code DISABLED} 停用。 */
    private MerchantOrgStatus status;

    /** 创建时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public MerchantOrgStatus getStatus() {
        return status;
    }

    public void setStatus(MerchantOrgStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
