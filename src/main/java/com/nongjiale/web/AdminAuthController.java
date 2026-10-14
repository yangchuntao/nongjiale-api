package com.nongjiale.web;

import com.nongjiale.domain.AdminUser;
import com.nongjiale.repository.AdminUserRepository;
import com.nongjiale.security.AuthPrincipal;
import com.nongjiale.security.SessionTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AdminUserRepository admins;
    private final PasswordEncoder encoder;
    private final SessionTokens tokens;

    public AdminAuthController(AdminUserRepository admins, PasswordEncoder encoder, SessionTokens tokens) {
        this.admins = admins;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    public record LoginReq(@NotBlank String username, @NotBlank String password) {}

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        AdminUser a = admins.findByUsername(req.username())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, 401, "用户名或密码错误"));
        if (!a.isEnabled() || !encoder.matches(req.password(), a.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "用户名或密码错误");
        }
        String token = tokens.issue(new AuthPrincipal(a.getId(), a.getUsername(), "ADMIN", a.getRole()));
        return ApiResponse.ok(Map.of(
                "token", token,
                "adminId", a.getId(),
                "username", a.getUsername(),
                "displayName", a.getDisplayName(),
                "role", a.getRole()
        ));
    }
}
