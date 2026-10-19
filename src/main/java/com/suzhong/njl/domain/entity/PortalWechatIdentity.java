package com.suzhong.njl.domain.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 游客微信身份绑定，对应 {@code portal_wechat_identity} 表。
 *
 * <p>记录小程序 {@code openid} 与门户游客账号的对应关系，由微信登录/绑定/解绑流程维护。
 * 同一 {@code appId + openid} 只能绑定一个游客，同一游客在同一 AppID 下也只保留一条记录；
 * 解绑只删当前 AppID 的记录，不影响网页版密码账号。
 *
 * <p>不存储 {@code session_key}，也不承担支付能力。
 */
@TableName("portal_wechat_identity")
public class PortalWechatIdentity {

    /** 主键，自增。 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 门户游客 ID，对应 {@code portal_user.id}，非空。 */
    private Long portalUserId;

    /** 微信小程序 AppID，非空，最长 32 字；与 {@code openid} 组成唯一键。 */
    private String appId;

    /** 小程序用户标识 openid，非空，最长 64 字；同一 AppID 内唯一。 */
    private String openid;

    /** 微信开放平台 unionid，可空，最长 64 字；未开通或未返回时为空。 */
    private String unionid;

    /** 绑定时间，插入时自动填充。 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPortalUserId() {
        return portalUserId;
    }

    public void setPortalUserId(Long portalUserId) {
        this.portalUserId = portalUserId;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getUnionid() {
        return unionid;
    }

    public void setUnionid(String unionid) {
        this.unionid = unionid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
