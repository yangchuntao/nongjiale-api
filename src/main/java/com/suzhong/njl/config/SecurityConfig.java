package com.suzhong.njl.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.suzhong.njl.domain.enums.AdminRole;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.ApiAccessDeniedHandler;
import com.suzhong.njl.security.ApiAuthenticationEntryPoint;
import com.suzhong.njl.security.BearerTokenFilter;

/**
 * Spring Security 主配置：无状态 Bearer 鉴权 + 路由级角色约束。
 *
 * <p>三端共用一条过滤链：
 * <ul>
 *   <li>游客：{@code /api/auth/**} 登录注册公开，{@code /api/me/**} 与提交意向要求 {@code TOURIST}；</li>
 *   <li>商家：{@code /api/merchant/**} 要求 {@code MERCHANT}；</li>
 *   <li>运营：{@code /api/admin/**} 要求 {@code OPERATOR} 或 {@code AGENT}；</li>
 *   <li>门户浏览、健康检查、微信登录公开。</li>
 * </ul>
 *
 * <p>令牌是进程内存实现，不用 Session、不用 Cookie，因此关闭 CSRF 与表单登录；
 * 认证失败返回 401、权限不足返回 403，响应体仍是统一信封。
 */
@Configuration
public class SecurityConfig {

    /**
     * 产出安全过滤链。
     *
     * @param http                  安全构建器
     * @param bearerTokenFilter     解析 Authorization: Bearer 的自定义过滤器
     * @param authenticationEntryPoint 未认证时的 401 输出
     * @param accessDeniedHandler   已认证但角色不符时的 403 输出
     * @return 配置完成的过滤链
     * @throws Exception 构建失败
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            BearerTokenFilter bearerTokenFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler) throws Exception {
        // 纯 Bearer、无 Cookie 会话，CSRF 无从附着；其余默认登录方式也一并关掉，只留自定义过滤器
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // 关闭匿名身份：未带令牌的请求不会变成「匿名已认证」，而是直接走 401 入口
                .anonymous(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // 匹配按声明顺序生效：登录接口必须先放行，否则会被下面的端级规则拦下
                        .requestMatchers(HttpMethod.POST, "/api/admin/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/merchant/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/wechat/login").permitAll()
                        // 绑定/解绑/打通状态需要微信渠道的游客令牌
                        .requestMatchers("/api/auth/wechat/**").hasAuthority(UserType.TOURIST.name())
                        // 健康检查与容器错误转发不鉴权，供探针和 404 信封使用
                        .requestMatchers("/health", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").hasAuthority(UserType.TOURIST.name())
                        .requestMatchers(HttpMethod.POST, "/api/booking-intents").hasAuthority(UserType.TOURIST.name())
                        .requestMatchers("/api/me/**").hasAuthority(UserType.TOURIST.name())
                        // 运营与城市代理共享后台接口，细粒度差异在 Service 内判断
                        .requestMatchers("/api/admin/**")
                        .hasAnyAuthority(AdminRole.OPERATOR.name(), AdminRole.AGENT.name())
                        .requestMatchers("/api/merchant/**").hasAuthority(UserType.MERCHANT.name())
                        // 门户浏览类接口公开；未知路径也放行，由 GlobalExceptionHandler 返回 404 而不是 401
                        .anyRequest().permitAll())
                // 在用户名密码过滤器之前解析令牌，保证后续授权规则能拿到 Authentication
                .addFilterBefore(bearerTokenFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** 密码统一用 BCrypt 存储与校验，种子里的演示口令也是 BCrypt 哈希。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 关掉 Boot 默认随机用户。运营登录走手机号接口，不走 UserDetailsService。
     */
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("请使用手机号登录");
        };
    }
}
