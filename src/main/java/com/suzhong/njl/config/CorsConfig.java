package com.suzhong.njl.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 跨域配置，服务三个本地前端（门户 43123、运营 43124、商家 43125）。
 *
 * <p>这里只注册 {@code CorsConfigurationSource} Bean，实际启用点在
 * {@code SecurityConfig} 的 {@code cors(Customizer.withDefaults())}；
 * 放在 Security 之前处理，预检请求不会先被鉴权拦截。
 */
@Configuration
public class CorsConfig {

    /**
     * 产出全局 CORS 规则，路径模式为 {@code /**}。
     *
     * <p>关键取舍：
     * <ul>
     *   <li>白名单来自 {@code app.cors-allowed-origins}，不用 {@code *}，避免任意站点调用；</li>
     *   <li>{@code allowCredentials=false}：鉴权走 Authorization 头里的 Bearer，不依赖 Cookie，
     *       关闭凭证同时规避「通配来源 + 携带凭证」的浏览器限制；</li>
     *   <li>预检结果缓存 1 小时，减少 OPTIONS 往返。</li>
     * </ul>
     *
     * @param appProperties 提供 CORS 白名单
     * @return 供 Spring Security 使用的跨域配置源
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(AppProperties appProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appProperties.getCorsAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        // 纯 Bearer 无 Cookie，不需要也不应允许携带凭证
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
