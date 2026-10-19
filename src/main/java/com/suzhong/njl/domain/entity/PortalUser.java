package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.FieldFill;

/**
 * 门户游客账号，对应 {@code portal_user} 表。
 *
 * <p>游客可用手机号 + 密码注册登录，也可通过微信小程序静默登录自动建号；由门户侧维护，
 * 运营和商家账号不在本表。
 *
 * <p>关键约定：手机号和密码均可空。微信壳用户初始没有手机号和密码，绑定手机号后才能
 * 提交预订意向；无密码账号走网页版密码登录会返回 401。
 */
@TableName("portal_user")
public class PortalUser {

    /** 主键，自增；被 {@code booking_intent.user_id}、{@code portal_wechat_identity.portal_user_id} 引用。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 手机号，11 位，唯一但可空；微信壳用户未绑定时为 {@code null}，绑定后与网页版账号打通。 */
    private String phone;

    /** BCrypt 密码摘要，可空；纯微信登录的壳用户没有密码。 */
    private String passwordHash;

    /** 游客展示名，可空，最长 64 字；注册未填时用脱敏手机号，微信壳账号默认「微信用户」。 */
    private String displayName;

    /** 创建时间，插入时自动填充。 */
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
