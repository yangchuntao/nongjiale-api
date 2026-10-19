package com.suzhong.njl.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 每请求解析一次 Bearer 令牌，并把登录主体写入 Spring Security 上下文。
 *
 * <p>职责：从 {@code Authorization} 头取出 {@code Bearer } 之后的原文（64 位十六进制），
 * 交给 {@link TokenStore} 换成 {@link AuthenticatedUser}；命中则构造
 * {@code UsernamePasswordAuthenticationToken} 放入 {@code SecurityContext}，未命中则什么都不做。
 *
 * <p>请求链路位置：注册在 {@code SecurityFilterChain} 中
 * {@code UsernamePasswordAuthenticationFilter} 之前，先于授权规则执行，
 * 因此 {@code hasAuthority(...)} 能读到本过滤器写入的认证信息。继承 {@code OncePerRequestFilter}，
 * 每个请求只执行一次（含 ERROR 转发时的默认跳过行为）。
 *
 * <p>生命周期：Spring 单例，字段只有无状态的 {@link TokenStore} 引用，可并发复用；
 * 真正的认证信息存放在请求线程绑定的 {@code SecurityContext} 中，不跨请求共享。
 *
 * <p>关键限制：解析失败（头缺失、前缀不符、令牌不存在或过期）时<strong>不在这里返回 401</strong>，
 * 而是直接放行。这样公开路径（如门户浏览、登录接口）仍可匿名访问；是否拒绝由后续授权规则决定，
 * 受保护路径最终由 {@link ApiAuthenticationEntryPoint} 统一输出 401。
 */
@Component
public class BearerTokenFilter extends OncePerRequestFilter {

    /** 令牌前缀，包含末尾空格；大小写敏感，与前端和文档约定一致。 */
    static final String PREFIX = "Bearer ";

    private final TokenStore tokenStore;

    public BearerTokenFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    /**
     * 解析令牌并尝试建立认证。
     *
     * <p>只处理同时满足「头非空」且「以 {@code Bearer } 开头」的请求；其余情况视为匿名，继续走过滤链。
     * 取出的原文会先 {@code trim}，避免前端多打空格导致查不到令牌。
     *
     * <p>只有 {@link TokenStore#resolve(String)} 返回主体时才写入 {@code SecurityContext}；
     * 写入时机在过滤链继续之前，保证后续授权与控制器能读到。令牌过期或已作废时视同未登录。
     *
     * @param request     当前请求，从 {@code Authorization} 头读取令牌
     * @param response    当前响应，本方法不直接写；401 由入口点负责
     * @param filterChain 后续过滤链，无论是否认证成功都必须继续调用
     * @throws ServletException 下游过滤器抛出
     * @throws IOException      下游读写抛出
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        // 前缀不匹配（含 Basic、裸令牌）一律当作未携带令牌，不抛异常、不提前中断
        if (header != null && header.startsWith(PREFIX)) {
            String raw = header.substring(PREFIX.length()).trim();
            // 查不到或已过期时 Optional 为空，保持匿名状态放行，由授权规则决定是否需要 401
            tokenStore.resolve(raw).ifPresent(user -> {
                // 主体即 AuthenticatedUser，凭据置 null：令牌本身已校验，无需再保存密码
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        // 单权限模型：运营取角色名，游客/商家取 UserType 名，与 SecurityConfig 的规则对应
                        List.of(new SimpleGrantedAuthority(user.authority())));
                // 必须在过滤链继续前写入，否则后续授权规则看不到本次登录
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
