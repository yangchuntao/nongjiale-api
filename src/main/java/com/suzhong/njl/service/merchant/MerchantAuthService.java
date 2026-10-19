package com.suzhong.njl.service.merchant;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.domain.entity.MerchantOrg;
import com.suzhong.njl.domain.entity.MerchantUser;
import com.suzhong.njl.domain.enums.MerchantOrgStatus;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.mapper.MerchantOrgMapper;
import com.suzhong.njl.mapper.MerchantUserMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.IssuedToken;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.web.ApiException;
import com.suzhong.njl.web.merchant.dto.MerchantLoginRequest;
import com.suzhong.njl.web.merchant.dto.MerchantLoginResponse;

/**
 * 商家端登录服务（商家端，公开接口 {@code POST /api/merchant/auth/login}）。
 *
 * <p>职责：用商家手机号 + 明文密码换取 Bearer 令牌，签发前同时校验账号启用与所属主体启用。
 * 令牌角色固定为 {@link UserType#MERCHANT}，只能访问 {@code /api/merchant/**}；服务层还会按
 * 令牌中的 {@code merchantOrgId} 限定本主体数据。
 *
 * <p>关键约定：
 * <ul>
 *   <li>手机号或密码错误统一返回 401，不区分「账号不存在」与「密码错误」，避免枚举账号；</li>
 *   <li>账号停用或主体停用返回 403，两者任一不满足都不签发令牌；</li>
 *   <li>令牌由 {@link TokenStore} 签发并保存在进程内存，不落库、不进 Redis，重启后全部失效；</li>
 *   <li>商家账号由运营审核入驻后开通，本端没有注册接口。</li>
 * </ul>
 *
 * <p>事务边界：只读校验 + 内存写令牌，不开启事务。
 */
@Service
public class MerchantAuthService {

    private final MerchantUserMapper merchantUserMapper;
    private final MerchantOrgMapper merchantOrgMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    public MerchantAuthService(
            MerchantUserMapper merchantUserMapper,
            MerchantOrgMapper merchantOrgMapper,
            PasswordEncoder passwordEncoder,
            TokenStore tokenStore) {
        this.merchantUserMapper = merchantUserMapper;
        this.merchantOrgMapper = merchantOrgMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
    }

    /**
     * 商家登录：手机号 + 密码换取商家令牌。
     *
     * <p>流程：按手机号查账号 → BCrypt 比对密码 → 校验账号 {@code enabled} → 校验所属主体为
     * {@code ACTIVE} → 以账号 ID、主体 ID、展示名、手机号组装 {@link UserType#MERCHANT} 主体 →
     * 签发令牌 → 返回令牌与账号/主体信息。
     *
     * @param request 登录请求，手机号与密码均非空；手机号格式由 DTO 校验
     * @return 令牌、过期时间、商家账号 ID、主体 ID、固定角色 {@code MERCHANT} 与展示名
     * @throws ApiException 账号不存在或密码不匹配时 401「手机号或密码错误」；
     *                      账号停用时 403「账号已停用」；主体不存在或非启用时 403「商家已停用」
     */
    public MerchantLoginResponse login(MerchantLoginRequest request) {
        // merchant_user.phone 有唯一约束，按手机号可确定唯一账号
        MerchantUser user = merchantUserMapper.selectOne(
                Wrappers.<MerchantUser>lambdaQuery().eq(MerchantUser::getPhone, request.phone()));
        // 账号不存在与密码错误合并成同一句提示，避免外部借错误文案枚举已开通的手机号
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "手机号或密码错误");
        }
        // 账号级开关：运营停用单个账号后禁止登录，历史令牌由 MerchantAccess 在每次请求时拦截
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "账号已停用");
        }
        // 主体级开关：主体停用后其下所有账号都不能登录，门店与历史数据保留
        MerchantOrg org = merchantOrgMapper.selectById(user.getMerchantOrgId());
        if (org == null || org.getStatus() != MerchantOrgStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "商家已停用");
        }
        // merchantOrgId 写入令牌，后续所有商家接口据此做主体隔离；adminRole 为空表示非运营身份
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(),
                UserType.MERCHANT,
                null,
                user.getMerchantOrgId(),
                user.getDisplayName(),
                user.getPhone());
        // 令牌写入进程内存的 TokenStore，不落库、不进 Redis；服务重启或令牌过期后需重新登录
        IssuedToken issued = tokenStore.issue(principal);
        // 响应里的角色固定 MERCHANT，前端据此进入商家工作台，不能由请求方指定
        return new MerchantLoginResponse(
                issued.token(),
                issued.expiresAt(),
                user.getId(),
                user.getMerchantOrgId(),
                UserType.MERCHANT,
                user.getDisplayName());
    }
}
