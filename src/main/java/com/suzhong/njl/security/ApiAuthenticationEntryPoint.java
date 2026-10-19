package com.suzhong.njl.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.suzhong.njl.web.ApiErrorWriter;

/**
 * 未认证访问受保护资源时的 401 出口。
 *
 * <p>职责：当请求没有携带 Bearer、令牌无法解析或已过期，且目标路径要求登录时，
 * Spring Security 回调本类，经 {@link ApiErrorWriter} 输出统一 JSON 信封
 * （{@code code=401}、{@code message=未登录}、{@code data=null}）。
 *
 * <p>请求链路位置：装配在 {@code SecurityFilterChain.exceptionHandling().authenticationEntryPoint(...)}。
 * {@link BearerTokenFilter} 解析失败时不会自行写 401，而是放行到授权阶段，由本类统一产出响应，
 * 这样公开路径仍可匿名访问，受保护路径才被拦下。
 *
 * <p>生命周期：Spring 单例、无状态，可并发复用。关键限制：响应一旦提交便无法再改状态码，
 * 因此本类只应在业务写响应前被触发。
 */
@Component
public class ApiAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ApiErrorWriter apiErrorWriter;

    public ApiAuthenticationEntryPoint(ApiErrorWriter apiErrorWriter) {
        this.apiErrorWriter = apiErrorWriter;
    }

    /**
     * 输出统一的 401 信封。
     *
     * <p>无论缺失令牌、令牌伪造还是令牌过期，对外都只说「未登录」，不区分原因，
     * 避免攻击者据此判断令牌是否曾经存在。
     *
     * @param request       当前请求，仅为框架契约，本实现不读取
     * @param response      当前响应，状态码与 JSON 正文由 {@link ApiErrorWriter} 写入
     * @param authException 触发本次入口的认证异常，本实现不读取其消息，防止内部细节外泄
     * @throws IOException 响应写出失败
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        apiErrorWriter.write(response, HttpStatus.UNAUTHORIZED, "未登录");
    }
}
