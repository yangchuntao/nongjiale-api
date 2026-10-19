package com.suzhong.njl.web.portal;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.security.CurrentUser;
import com.suzhong.njl.service.portal.WechatAuthService;
import com.suzhong.njl.web.ApiResponse;
import com.suzhong.njl.web.portal.dto.WechatAuthResponse;
import com.suzhong.njl.web.portal.dto.WechatBindRequest;
import com.suzhong.njl.web.portal.dto.WechatLinkResponse;
import com.suzhong.njl.web.portal.dto.WechatLoginRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 门户微信小程序登录与账号打通接口（{@code /api/auth/wechat}）。
 *
 * <p>所属端：门户游客。{@code /login} 公开；绑定、解绑、打通状态需 {@code TOURIST} 角色 Bearer 令牌，
 * 且绑定/解绑要求令牌来自微信渠道（带 openid）。
 *
 * <p>未配置小程序 AppID/Secret 时，本控制器全部接口返回 503「微信登录未配置」，进程仍可启动。
 * 响应不含 {@code session_key}，openid 只参与服务端身份匹配与令牌作废。
 */
@Tag(name = "门户 · 微信登录", description = "微信小程序静默登录与账号绑定打通")
@RestController
@RequestMapping("/api/auth/wechat")
public class WechatAuthController {

    private final WechatAuthService wechatAuthService;

    public WechatAuthController(WechatAuthService wechatAuthService) {
        this.wechatAuthService = wechatAuthService;
    }

    /**
     * {@code POST /api/auth/wechat/login} — 微信小程序静默登录（公开）。
     *
     * <p>用 {@code wx.login} 的 code 调用 {@code jscode2session}；同一 openid 再次登录复用原用户，
     * 首次登录创建无手机号、无密码的壳账号。响应含手机号与绑定/打通状态，不含 {@code session_key}。
     *
     * @param request 登录请求，只含 {@code code}；必填、非空白且不超过 128 字，未加 {@code @Valid}，由服务层校验
     * @return 游客令牌、用户 ID、展示名、手机号、手机号绑定、微信绑定与账号打通状态
     * @throws com.suzhong.njl.web.ApiException 未配置 503；code 缺失/超长或请求体格式错误 400；
     *                                         微信未返回 openid 502；账号异常 401
     */
    @Operation(summary = "微信静默登录", description = "未配置小程序 AppID/Secret 返回 503；同一 openid 复用原用户，首次登录创建无手机号的壳账号。")
    @PostMapping("/login")
    public ApiResponse<WechatAuthResponse> login(@RequestBody WechatLoginRequest request) {
        return ApiResponse.ok(wechatAuthService.login(request));
    }

    /**
     * {@code POST /api/auth/wechat/bind} — 把当前微信身份绑定到手机号账号。
     *
     * <p>入参二选一：{@code phoneCode}（微信手机号凭证），或 {@code phone} + {@code password}；
     * 两者同时给或都不给都是 400。手机号已有账号时按需校验密码并把微信身份迁过去，
     * 新手机号时密码必填（6 到 64 位）。身份迁到已有账号时，旧 openid 令牌立即失效。
     *
     * @param authentication 当前游客令牌，必须来自微信渠道且带 openid
     * @param request        手机号凭证，或手机号加密码；未知字段会被拒绝
     * @return 目标账号的令牌与绑定/打通状态
     * @throws com.suzhong.njl.web.ApiException 未配置 503；非微信令牌、入参非法或密码长度错误 400；
     *                                         密码错误或身份归属异常 401；已绑其他手机号、手机号已绑其他微信、
     *                                         壳账号已有意向 409
     */
    @Operation(summary = "绑定手机号", description = "phoneCode 与手机号加密码二选一，同时给或都不给返回 400；身份迁移到已有账号后旧 openid 令牌立即失效。")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/bind")
    public ApiResponse<WechatAuthResponse> bind(Authentication authentication, @RequestBody WechatBindRequest request) {
        return ApiResponse.ok(wechatAuthService.bind(CurrentUser.require(authentication, UserType.TOURIST), request));
    }

    /**
     * {@code POST /api/auth/wechat/unbind} — 解除当前微信与账号的绑定。
     *
     * <p>只删除「当前 AppID + 当前 openid」这一条身份，并作废带这枚 openid 的内存令牌；
     * 同一账号的网页版密码令牌继续有效。解绑后不会自动删除账号。
     *
     * @param authentication 当前游客令牌，必须来自微信渠道
     * @return 解绑后的打通状态，通常 {@code wechatBound=false}
     * @throws com.suzhong.njl.web.ApiException 未配置 503；非微信令牌 400；未登录 401；角色不是游客 403
     */
    @Operation(summary = "解绑微信", description = "只删除当前 AppID 与 openid 这一条身份并作废该 openid 令牌；同账号的密码令牌继续有效，账号不会被删除。")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/unbind")
    public ApiResponse<WechatLinkResponse> unbind(Authentication authentication) {
        return ApiResponse.ok(wechatAuthService.unbind(CurrentUser.require(authentication, UserType.TOURIST)));
    }

    /**
     * {@code GET /api/auth/wechat/link} — 查询账号打通状态（只读）。
     *
     * <p>手机号已绑、密码已设、微信已绑三者同时成立时 {@code accountLinked=true}；
     * 手机号渠道的游客令牌也可调用，不发新令牌、不解绑。
     *
     * @param authentication 当前游客令牌
     * @return 用户 ID、手机号、手机号绑定、微信绑定与账号打通状态
     * @throws com.suzhong.njl.web.ApiException 未配置 503；未登录 401；角色不是游客 403；账号不存在 401
     */
    @Operation(summary = "打通状态查询", description = "手机号已绑、密码已设、微信已绑三者同时成立时 accountLinked=true；只读，不发新令牌。")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/link")
    public ApiResponse<WechatLinkResponse> link(Authentication authentication) {
        return ApiResponse.ok(wechatAuthService.link(CurrentUser.require(authentication, UserType.TOURIST)));
    }
}
