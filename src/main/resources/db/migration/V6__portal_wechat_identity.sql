-- 微信小程序身份。壳用户允许没有手机号和密码。
-- 唯一键允许多个 NULL 手机号，禁止用空串占位。
-- 不建支付表，不存 session_key。

ALTER TABLE portal_user MODIFY COLUMN phone VARCHAR(20) NULL;
ALTER TABLE portal_user MODIFY COLUMN password_hash VARCHAR(100) NULL;

CREATE TABLE portal_wechat_identity (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    portal_user_id BIGINT NOT NULL,
    app_id VARCHAR(32) NOT NULL,
    openid VARCHAR(64) NOT NULL,
    unionid VARCHAR(64) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_portal_wechat_identity_user FOREIGN KEY (portal_user_id) REFERENCES portal_user (id),
    CONSTRAINT uk_portal_wechat_identity_app_openid UNIQUE (app_id, openid),
    CONSTRAINT uk_portal_wechat_identity_user_app UNIQUE (portal_user_id, app_id)
);
