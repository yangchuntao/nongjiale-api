package com.suzhong.njl.security;

import java.time.Instant;

/**
 * 令牌签发结果，登录接口直接把这两个字段返回给客户端。
 *
 * <p>生命周期：由 {@link TokenStore#issue(AuthenticatedUser)} 创建，随响应返回后即不再被服务端持有——
 * 服务端只保存令牌到主体的映射，{@code expiresAt} 仅用于告知客户端何时需要重新登录。
 *
 * <p>关键限制：{@code token} 是明文令牌，不含 {@code Bearer } 前缀；不要写入日志或错误消息。
 * 载荷刻意只有令牌与过期时刻，{@code session_key}、密码哈希等敏感数据不进入令牌——
 * 本项目的令牌不是 JWT，没有自描述载荷，也不需要在客户端保存任何账号信息。
 *
 * @param token     令牌原文，32 字节随机数的十六进制表示（64 字符）
 * @param expiresAt 绝对过期时刻，由签发时的应用时钟加 TTL 得出；到点后该令牌等同于未登录
 */
public record IssuedToken(String token, Instant expiresAt) {
}
