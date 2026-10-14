package com.nongjiale.config;

import com.nongjiale.security.TokenAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${nongjiale.cors-allowed-origins}")
    private String corsOrigins;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, TokenAuthFilter tokenAuthFilter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(401);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":401,\"message\":\"未登录\",\"data\":null}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(403);
                            res.setContentType("application/json;charset=UTF-8");
                            res.getWriter().write("{\"code\":403,\"message\":\"无权限\",\"data\":null}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**", "/api/admin/auth/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/regions", "/api/home/**", "/api/homestays/**", "/api/articles/**",
                                "/api/config/**").permitAll()
                        .requestMatchers("/api/admin/**").hasAnyRole("OPERATOR", "AGENT")
                        .requestMatchers(HttpMethod.POST, "/api/booking-intents", "/api/contact-messages")
                            .hasRole("TOURIST")
                        .anyRequest().authenticated()
                )
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(corsOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
