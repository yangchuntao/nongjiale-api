package com.suzhong.njl.service.portal;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.IssuedToken;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.service.support.PhoneMask;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.portal.dto.PortalAuthResponse;
import com.suzhong.njl.web.portal.dto.PortalLoginRequest;
import com.suzhong.njl.web.portal.dto.PortalMeResponse;
import com.suzhong.njl.web.portal.dto.PortalRegisterRequest;

/**
 * 游客账号服务（门户端手机号渠道）。
 *
 * <p>职责：注册、手机号 + 密码登录、查询当前游客身份。令牌由 {@link TokenStore} 发在进程内存，
 * 不进 Redis，也不写数据库。
 *
 * <p>关键约束：
 * <ul>
 *   <li>手机号全局唯一：先查一次给出友好 409，并发窗口内再靠唯一索引兜底；</li>
 *   <li>没有密码（如微信壳用户）的账号不能用密码登录，返回 401，不做空指针式比较；</li>
 *   <li>令牌角色固定 {@code TOURIST}，只能访问游客接口；具体过期时间见 {@code app.security.token-ttl}。</li>
 * </ul>
 *
 * <p>注册是一次插入，无显式事务；唯一键冲突由数据库保证。
 */
@Service
public class PortalAuthService {

    private final PortalUserMapper portalUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    public PortalAuthService(PortalUserMapper portalUserMapper, PasswordEncoder passwordEncoder, TokenStore tokenStore) {
        this.portalUserMapper = portalUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
    }

    /**
     * 注册游客账号并直接签发令牌。
     *
     * <p>流程：按手机号查重 → 密码 BCrypt 加密 → 展示名为空时用脱敏手机号 → 插入 → 签发令牌。
     *
     * @param request 注册请求，手机号与密码由控制器/DTO 校验
     * @return 新账号的令牌与基础信息
     * @throws ApiException 手机号已存在时 409「手机号已注册」（含并发下的唯一键冲突）
     */
    public PortalAuthResponse register(PortalRegisterRequest request) {
        PortalUser existing = portalUserMapper.selectOne(
                Wrappers.<PortalUser>lambdaQuery().eq(PortalUser::getPhone, request.phone()));
        if (existing != null) {
            throw new ApiException(HttpStatus.CONFLICT, "手机号已注册");
        }
        PortalUser user = new PortalUser();
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        String displayName = request.displayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = PhoneMask.mask(request.phone());
        }
        user.setDisplayName(displayName);
        try {
            portalUserMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // 先查后插之间存在并发窗口，这里把唯一索引冲突翻译成同一个 409
            throw new ApiException(HttpStatus.CONFLICT, "手机号已注册");
        }
        return issue(user);
    }

    /**
     * 手机号 + 密码登录。
     *
     * <p>账号不存在、密码哈希为空（微信壳账号未设密码）、密码不匹配，三种情况统一返回 401，
     * 不区分文案，避免被用来探测手机号是否注册。
     *
     * @param request 登录请求
     * @return 新签发的令牌与账号信息
     * @throws ApiException 认证失败时 401「手机号或密码错误」
     */
    public PortalAuthResponse login(PortalLoginRequest request) {
        PortalUser user = portalUserMapper.selectOne(
                Wrappers.<PortalUser>lambdaQuery().eq(PortalUser::getPhone, request.phone()));
        if (user == null || !passwordMatches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "手机号或密码错误");
        }
        return issue(user);
    }

    /**
     * 查询当前登录游客的身份。
     *
     * <p>令牌有效但账号已被删除（例如微信绑定合并时清理壳用户）时按未登录处理。
     *
     * @param user 当前令牌解析出的主体
     * @return 用户 ID、手机号、展示名与固定角色
     * @throws ApiException 账号已不存在时 401「未登录」
     */
    public PortalMeResponse me(AuthenticatedUser user) {
        PortalUser stored = portalUserMapper.selectById(user.userId());
        if (stored == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return new PortalMeResponse(stored.getId(), stored.getPhone(), stored.getDisplayName(), UserType.TOURIST);
    }

    /** 校验密码：原文或哈希为空直接判失败，避免未设密码的账号被空密码放行。 */
    private boolean passwordMatches(String raw, String hash) {
        if (raw == null || hash == null || hash.isBlank()) {
            return false;
        }
        return passwordEncoder.matches(raw, hash);
    }

    /** 组装游客主体并签发内存令牌，返回给前端的只有令牌与展示信息。 */
    private PortalAuthResponse issue(PortalUser user) {
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                UserType.TOURIST,
                null,
                null,
                user.getDisplayName(),
                user.getPhone());
        IssuedToken issued = tokenStore.issue(principal);
        return new PortalAuthResponse(
                issued.token(),
                issued.expiresAt(),
                user.getId(),
                UserType.TOURIST,
                user.getDisplayName());
    }
}
