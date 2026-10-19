package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 商家登录账号，对应 {@code merchant_user} 表。
 *
 * <p>由运营在入驻审核通过时开通，一个账号绑定一个商家主体；商家端只能访问
 * {@code /api/merchant/**}，且数据范围限制在本主体内。
 *
 * <p>关键约束：手机号全局唯一；密码只存 BCrypt 摘要；账号或所属主体任一停用，
 * 登录都会被拒，已有令牌失效。
 */
@TableName("merchant_user")
public class MerchantUser {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属商家主体 ID，对应 {@code merchant_org.id}，非空；决定商家端可见的门店和意向范围。 */
    private Long merchantOrgId;

    /** 登录手机号，11 位，全局唯一；审核通过时取入驻申请的联系电话。 */
    private String phone;

    /** BCrypt 密码摘要，非空；运营重置密码时覆盖，明文只在当次响应返回。 */
    private String passwordHash;

    /** 账号展示名，可空，最长 64 字；优先取申请联系人姓名，没有则取主体名称。 */
    private String displayName;

    /** 是否启用，非空，默认 {@code true}；停用后不能登录，旧令牌也不再放行。 */
    private Boolean enabled;

    /** 创建时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMerchantOrgId() {
        return merchantOrgId;
    }

    public void setMerchantOrgId(Long merchantOrgId) {
        this.merchantOrgId = merchantOrgId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
