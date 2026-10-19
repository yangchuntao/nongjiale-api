package com.suzhong.njl.web;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 统一响应信封，所有业务接口（健康检查除外）都返回这个结构。
 *
 * <p>约定：
 * <ul>
 *   <li>{@code code}：0 表示成功；失败时等于 HTTP 状态码（400/401/403/404/409/410/500/503），
 *       前端只需判断 0 与非 0；</li>
 *   <li>{@code message}：成功固定 {@code ok}，失败为可直接展示的中文提示；</li>
 *   <li>{@code data}：成功时的业务数据，失败时为 null，类型由各接口决定。</li>
 * </ul>
 *
 * <p>HTTP 状态码与 {@code code} 保持一致，这样网关、监控和前端都能按任一维度判断。
 *
 * @param code    业务码，0 为成功，非 0 时与 HTTP 状态码一致
 * @param message 结果说明
 * @param data    业务数据，失败时为 null
 * @param <T>     数据类型
 */
// 不要在这里写 @Schema(name = "ApiResponse")：泛型 record 一旦固定 schema 名，
// springdoc 会把所有接口的 data 都指向同一个具体类型，导致响应结构串台。
@Schema(description = "统一响应信封；除 /health 外的业务接口统一使用此结构，code 与 HTTP 状态码保持一致")
public record ApiResponse<T>(
        @Schema(description = "业务码：0 表示成功；非 0 时与 HTTP 状态码一致（400/401/403/404/409/410/500/503）", example = "0")
        int code,
        @Schema(description = "提示文案：成功固定 ok，失败为可直接展示的中文提示", example = "ok")
        String message,
        @Schema(description = "业务数据：成功时由各接口决定类型，失败时为 null")
        T data) {

    /** 成功信封：{@code code=0}、{@code message=ok}。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "ok", data);
    }

    /** 失败信封：data 固定为 null，code 与 HTTP 状态码一致。 */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
