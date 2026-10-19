package com.suzhong.njl.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.suzhong.njl.web.ApiErrorWriter;

/**
 * 已认证但权限不足时的 403 出口。
 *
 * <p>职责：Spring Security 在 {@link AccessDeniedException} 冒泡到过滤链末端时回调本类，
 * 由 {@link ApiErrorWriter} 写出与业务接口一致的 JSON 信封（{@code code=403}、{@code message=无权限}、
 * {@code data=null}），避免浏览器拿到 HTML 错误页或空响应体。
 *
 * <p>请求链路位置：装配在 {@code SecurityFilterChain.exceptionHandling().accessDeniedHandler(...)}，
 * 与 {@link ApiAuthenticationEntryPoint} 成对使用——本类对应「令牌有效但角色不够」，后者对应「没有有效令牌」。
 *
 * <p>生命周期：Spring 单例、无状态，可被并发请求复用；仅在过滤器线程内同步写响应。
 * 关键限制：若响应已经提交（例如下游已写出内容），再进入本类无法改写状态码，
 * 因此鉴权必须发生在任何业务响应写出之前。
 */
@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {

    private final ApiErrorWriter apiErrorWriter;

    public ApiAccessDeniedHandler(ApiErrorWriter apiErrorWriter) {
        this.apiErrorWriter = apiErrorWriter;
    }

    /**
     * 输出统一的 403 信封。
     *
     * <p>不区分具体缺失的角色或资源归属，统一提示「无权限」，避免向调用方暴露后台权限模型。
     *
     * @param request               当前请求，仅为框架契约，本实现不读取
     * @param response              当前响应，状态码与 JSON 正文由 {@link ApiErrorWriter} 写入
     * @param accessDeniedException 触发拒绝的异常，本实现不读取其消息，防止内部细节外泄
     * @throws IOException 响应写出失败
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        apiErrorWriter.write(response, HttpStatus.FORBIDDEN, "无权限");
    }
}
