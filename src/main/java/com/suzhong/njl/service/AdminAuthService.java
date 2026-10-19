package com.suzhong.njl.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.AdminUser;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.mapper.AdminUserMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.IssuedToken;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.admin.dto.AdminLoginRequest;
import com.suzhong.njl.web.admin.dto.AdminLoginResponse;

/**
 * 运营端登录服务。
 *
 * <p>职责：校验运营/代理账号的手机号与密码，签发 {@code /api/admin/**} 可用的 Bearer 令牌。
 * 令牌只存进程内存，重启即失效，不进 Redis、不落库。
 *
 * <p>关键约束：
 * <ul>
 *   <li>先校验密码再校验启用状态：已停用账号即便密码正确也返回 403「账号已停用」；</li>
 *   <li>令牌角色固定 {@code OPERATOR}，具体是运营还是代理由 {@code admin_user.role} 决定，
 *       鉴权时两者都允许访问运营接口；</li>
 *   <li>无事务、无缓存；账号查询按手机号唯一命中。</li>
 * </ul>
 */
@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    public AdminAuthService(AdminUserMapper adminUserMapper, PasswordEncoder passwordEncoder, TokenStore tokenStore) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
    }

    /**
     * 运营端登录。
     *
     * <p>流程：按手机号查账号 → BCrypt 校验密码 → 校验启用状态 → 组装主体 → 签发内存令牌。
     *
     * @param request 手机号 + 明文密码
     * @return 令牌、过期时间与账号信息；明文密码不会回显
     * @throws ApiException 账号不存在或密码错误 401「手机号或密码错误」；账号已停用 403「账号已停用」
     */
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser user = adminUserMapper.selectOne(
                Wrappers.<AdminUser>lambdaQuery().eq(AdminUser::getPhone, request.phone()));
        // 账号不存在与密码错误使用同一文案，避免被用来枚举运营手机号
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "手机号或密码错误");
        }
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号已停用");
        }
        // 角色取库里的 OPERATOR / AGENT，但令牌主体类型统一为 OPERATOR，表示「可进运营端」
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                UserType.OPERATOR,
                user.getRole(),
                null,
                user.getDisplayName(),
                user.getPhone());
        IssuedToken issued = tokenStore.issue(principal);
        return new AdminLoginResponse(
                issued.token(),
                issued.expiresAt(),
                user.getId(),
                user.getRole(),
                user.getDisplayName());
    }
}
