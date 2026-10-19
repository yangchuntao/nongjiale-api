package com.suzhong.njl.wechat;

/**
 * 微信 HTTP 调用的原始结果，只承载状态码与响应体。
 *
 * <p>生命周期：由 {@link WechatWire} 每次调用新建，属于瞬时值对象，不缓存、不持久化。
 * 刻意保持中立：非 2xx 也会照常返回，是否视为失败、如何映射错误码由
 * {@link HttpWechatMiniappClient} 内部判断。
 *
 * <p>关键限制：{@code body} 可能包含微信原始 errmsg，仅用于本次解析，不得写日志或透传给客户端。
 *
 * @param status HTTP 状态码，未经任何判断
 * @param body   响应体原文，已按 UTF-8 解码；可能为 {@code null} 或空串
 */
public record WechatHttpResult(int status, String body) {
}
