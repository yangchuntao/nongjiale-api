package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.suzhong.njl.domain.enums.AdminRole;

/**
 * 运营后台账号，对应 {@code admin_user} 表。
 *
 * <p>承载运营（{@code OPERATOR}）与客服（{@code AGENT}）的登录身份，由运营侧维护；
 * 门户游客和商家账号分别落在 {@code portal_user}、{@code merchant_user}，不要混用。
 *
 * <p>关键约束：手机号全局唯一；密码只存 BCrypt 摘要；{@code enabled=false} 时登录被拒，
 * 已有 Bearer 令牌在后续鉴权中同样失效。
 */
@TableName("admin_user")
public class AdminUser {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录手机号，11 位，全局唯一；按原样存储并用于登录匹配。 */
    private String phone;

    /** BCrypt 密码摘要，非空；禁止存明文。本地种子数据的明文口令为 123456。 */
    private String passwordHash;

    /** 后台展示名，非空，用于审计和页面右上角身份显示。 */
    private String displayName;

    /** 运营角色，枚举 {@link AdminRole}：{@code OPERATOR} 运营、{@code AGENT} 客服；决定后台权限范围。 */
    private AdminRole role;

    /** 是否启用，非空，默认 {@code true}；停用后不能登录，旧令牌也不再放行。 */
    private Boolean enabled;

    /** 创建时间，插入时由 MyBatis-Plus 自动填充，库侧默认 {@code CURRENT_TIMESTAMP}。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AdminRole getRole() {
        return role;
    }

    public void setRole(AdminRole role) {
        this.role = role;
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
