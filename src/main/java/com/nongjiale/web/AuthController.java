package com.nongjiale.web;

import com.nongjiale.domain.AppUser;
import com.nongjiale.repository.AppUserRepository;
import com.nongjiale.security.AuthPrincipal;
import com.nongjiale.security.SessionTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final SessionTokens tokens;

    public AuthController(AppUserRepository users, PasswordEncoder encoder, SessionTokens tokens) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
    }

    public record LoginReq(@NotBlank String phone, @NotBlank String password) {}
    public record RegisterReq(@NotBlank String phone, @NotBlank String password, @NotBlank String displayName) {}

    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterReq req) {
        if (users.findByPhone(req.phone()).isPresent()) {
            throw new BusinessException(HttpStatus.CONFLICT, 409, "手机号已注册");
        }
        AppUser u = new AppUser();
        u.setPhone(req.phone());
        u.setDisplayName(req.displayName());
        u.setPasswordHash(encoder.encode(req.password()));
        users.save(u);
        String token = tokens.issue(new AuthPrincipal(u.getId(), u.getPhone(), "TOURIST", "TOURIST"));
        return ApiResponse.ok(Map.of(
                "token", token,
                "userId", u.getId(),
                "phone", u.getPhone(),
                "displayName", u.getDisplayName()
        ));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginReq req) {
        AppUser u = users.findByPhone(req.phone())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, 401, "手机号或密码错误"));
        if (!encoder.matches(req.password(), u.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "手机号或密码错误");
        }
        String token = tokens.issue(new AuthPrincipal(u.getId(), u.getPhone(), "TOURIST", "TOURIST"));
        return ApiResponse.ok(Map.of(
                "token", token,
                "userId", u.getId(),
                "phone", u.getPhone(),
                "displayName", u.getDisplayName()
        ));
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me(@RequestAttribute(value = "principal", required = false) Object ignored,
                                               org.springframework.security.core.Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof AuthPrincipal p) || !p.isTourist()) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, 401, "未登录");
        }
        AppUser u = users.findById(p.id())
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, 401, "未登录"));
        return ApiResponse.ok(Map.of(
                "userId", u.getId(),
                "phone", u.getPhone(),
                "displayName", u.getDisplayName()
        ));
    }
}
