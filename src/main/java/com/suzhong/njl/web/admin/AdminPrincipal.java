package com.suzhong.njl.web.admin;

import org.springframework.security.core.Authentication;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;

/**
 * 运营身份解析工具（包级私有，不可实例化）。
 *
 * <p>所属端：运营后台。说明：本类不是 Spring 的 {@code @AuthenticationPrincipal} 注入载体，
 * 也没有实例字段；它只是控制器里从 {@link Authentication} 取运营 ID 的静态入口。
 *
 * <p>取到的 ID 口径：运营账号主键，来自令牌主体 {@code AuthenticatedUser.userId()}，
 * 主体类型必须是 {@code OPERATOR}（{@code OPERATOR} 与 {@code AGENT} 两种后台角色登录后都是该类型），
 * 否则抛 401「未登录」或 403「无权限」。
 */
final class AdminPrincipal {

    private AdminPrincipal() {
    }

    /**
     * 从当前请求的安全上下文取运营账号 ID。
     *
     * <p>用于审计字段、创建人/审核人；未登录或主体类型不是运营时直接抛出，不返回空值。
     *
     * @param authentication Spring Security 注入的当前认证对象，可为空
     * @return 运营账号主键，写入 {@code created_by}/{@code updated_by} 或审计记录
     * @throws com.suzhong.njl.web.ApiException 未登录 401；主体类型不是 {@code OPERATOR} 403
     */
    static long adminId(Authentication authentication) {
        return CurrentUser.require(authentication, UserType.OPERATOR).userId();
    }
}
