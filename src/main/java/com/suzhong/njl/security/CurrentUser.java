package com.suzhong.njl.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.web.ApiException;

/**
 * 从 Spring Security 的 {@code Authentication} 中取当前登录人的工具。
 *
 * <p>职责：控制器方法直接接收 {@code Authentication} 参数，再调用本类做「是否已登录」与
 * 「是否属于指定用户端」的双重校验，返回强类型的 {@link AuthenticatedUser}，
 * 避免各处重复做 {@code instanceof} 与空值判断。
 *
 * <p>生命周期：纯静态工具类，禁止实例化，无状态、线程安全。
 *
 * <p>关键限制：本类只判断 {@link UserType} 大类，不做资源归属校验（例如商家是否拥有该门店），
 * 归属判断仍由各 Service 负责。未登录抛 401，角色不符抛 403；两种异常都由全局异常处理包成统一信封。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    /**
     * 取当前登录人，并校验其用户端类型。
     *
     * <p>判断顺序：先看认证是否存在、principal 是否为 {@link AuthenticatedUser}（未登录或令牌无效），
     * 再看 {@link UserType} 是否与期望一致（越权）。两者返回不同状态码，便于前端区分「重新登录」与「无权限」。
     *
     * @param authentication 由 Spring Security 注入的当前认证，可为 {@code null}
     * @param userType       期望的用户端类型，如 {@code TOURIST}、{@code MERCHANT}、{@code OPERATOR}
     * @return 当前登录人快照，非空
     * @throws ApiException 未登录或 principal 类型不符时 401「未登录」；用户端类型不符时 403「无权限」
     */
    public static AuthenticatedUser require(Authentication authentication, UserType userType) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            // 未带令牌、令牌过期，或匿名/其他认证方式产生的 principal，一律按未登录处理
            throw new ApiException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        if (user.userType() != userType) {
            // 令牌本身有效，只是走错了端（例如游客令牌访问商家接口）
            throw new ApiException(HttpStatus.FORBIDDEN, "无权限");
        }
        return user;
    }
}
