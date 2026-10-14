package com.nongjiale.security;

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

import java.io.IOException;
import java.util.List;

@Component
public class TokenAuthFilter extends OncePerRequestFilter {
    private final SessionTokens sessionTokens;

    public TokenAuthFilter(SessionTokens sessionTokens) {
        this.sessionTokens = sessionTokens;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            sessionTokens.resolve(header.substring(7)).ifPresent(p -> {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" +
                        (p.isTourist() ? "TOURIST" : p.role())));
                var auth = new UsernamePasswordAuthenticationToken(p, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);
            });
        }
        chain.doFilter(request, response);
    }
}
