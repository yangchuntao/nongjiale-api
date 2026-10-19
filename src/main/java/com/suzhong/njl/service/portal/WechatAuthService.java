package com.suzhong.njl.service.portal;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.suzhong.njl.config.WechatMiniappProperties;
import com.suzhong.njl.domain.entity.BookingIntent;
import com.suzhong.njl.domain.entity.PortalUser;
import com.suzhong.njl.domain.entity.PortalWechatIdentity;
import com.suzhong.njl.domain.enums.LoginChannel;
import com.suzhong.njl.domain.enums.UserType;
import com.suzhong.njl.domain.enums.WechatMessage;
import com.suzhong.njl.mapper.BookingIntentMapper;
import com.suzhong.njl.mapper.PortalUserMapper;
import com.suzhong.njl.mapper.PortalWechatIdentityMapper;
import com.suzhong.njl.security.AuthenticatedUser;
import com.suzhong.njl.security.IssuedToken;
import com.suzhong.njl.security.TokenStore;
import com.suzhong.njl.web.portal.dto.WechatAuthResponse;
import com.suzhong.njl.web.portal.dto.WechatBindRequest;
import com.suzhong.njl.web.portal.dto.WechatLinkResponse;
import com.suzhong.njl.web.portal.dto.WechatLoginRequest;
import com.suzhong.njl.wechat.WechatMiniappClient;
import com.suzhong.njl.wechat.WechatMiniappClient.JsCodeSession;

/**
 * 微信小程序登录与账号打通服务（门户端微信渠道）。
 *
 * <p>职责：用 {@code wx.login} 的 code 换游客令牌；把微信身份绑定到手机号账号；解除当前 AppID 的绑定；
 * 查询打通状态。微信协议字段的解析在 {@link WechatMiniappClient}，本类只处理业务分支。
 *
 * <p>关键约束：
 * <ul>
 *   <li>同一 openid 再次登录复用原来的 {@code portalUserId}，不重复建壳用户；</li>
 *   <li>{@code session_key} 与 access_token 在微信客户端层即丢弃，不落库、不写日志、不出现在响应里；</li>
 *   <li>绑定成功可能把身份迁移到目标用户并换发目标用户令牌，旧 openid 下的内存令牌要作废；
 *       解绑只撤销带这枚 openid 的令牌，网页版密码令牌保留；</li>
 *   <li>事务边界：登录、绑定、解绑各自用 {@link TransactionTemplate} 包住写库，缓存与令牌操作放在事务外；</li>
 *   <li>未配置 AppID/Secret 时统一返回 503「微信登录未配置」，进程仍可启动。</li>
 * </ul>
 */
@Service
public class WechatAuthService {

    private static final int PASSWORD_MIN = 6;
    private static final int PASSWORD_MAX = 64;

    private final PortalUserMapper portalUserMapper;
    private final PortalWechatIdentityMapper identityMapper;
    private final BookingIntentMapper bookingIntentMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;
    private final WechatMiniappClient wechatMiniappClient;
    private final WechatMiniappProperties properties;
    private final TransactionTemplate transactionTemplate;

    public WechatAuthService(
            PortalUserMapper portalUserMapper,
            PortalWechatIdentityMapper identityMapper,
            BookingIntentMapper bookingIntentMapper,
            PasswordEncoder passwordEncoder,
            TokenStore tokenStore,
            WechatMiniappClient wechatMiniappClient,
            WechatMiniappProperties properties,
            PlatformTransactionManager transactionManager) {
        this.portalUserMapper = portalUserMapper;
        this.identityMapper = identityMapper;
        this.bookingIntentMapper = bookingIntentMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenStore = tokenStore;
        this.wechatMiniappClient = wechatMiniappClient;
        this.properties = properties;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * 微信小程序静默登录。
     *
     * <p>流程：校验配置 → 取 code → 调 {@code jscode2session} → 按 openid 查身份（有则复用用户，
     * 无则新建壳用户并落身份）→ 补写 unionid → 签发微信渠道令牌。
     *
     * <p>并发说明：两个请求同时首登同一 openid 时，后插入的一方会撞唯一键，
     * 捕获后重新按 openid 查一次，拿到对方已建好的身份。
     *
     * @param request 登录请求，只含 {@code wx.login} 的 code
     * @return 令牌 + 用户 ID + 手机号/绑定/打通状态；响应不含 session_key
     * @throws ApiException 未配置 503；code 缺失或过长 400；微信侧未返回 openid 502；账号异常 401
     */
    public WechatAuthResponse login(WechatLoginRequest request) {
        requireConfigured();
        String code = normalizeLoginCode(request);
        JsCodeSession session = wechatMiniappClient.codeToSession(code);
        // 微信没返回 openid（code 过期、网络失败等）按 502 处理，不把 null 带进业务层
        if (session == null || !hasText(session.openid())) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
        PortalUser user;
        try {
            user = transactionTemplate.execute(status -> persistLogin(session));
        } catch (DuplicateKeyException ex) {
            // 并发首登：另一请求已插入同一 openid，重查即可复用，不把冲突抛给前端
            user = transactionTemplate.execute(status -> loadAfterConflict(session));
            if (user == null) {
                throw ex;
            }
        }
        return toResponse(user, session.openid());
    }

    /**
     * 把当前微信身份绑定到手机号账号（账号打通）。
     *
     * <p>入参二选一：手填手机号 + 密码，或微信手机号凭证 {@code phoneCode}；两者同时出现或都不给都是 400。
     * 手机号凭证会先向微信换真实号码，再走同一套绑定逻辑。
     *
     * <p>流程：校验配置与微信渠道令牌 → 解析入参 → 解析目标手机号 → 事务内完成绑定/合并 →
     * 需要时作废旧 openid 令牌 → 换发目标用户令牌。
     *
     * @param principal 当前令牌主体，必须是微信渠道且带 openid
     * @param request   绑定请求
     * @return 目标账号的令牌与绑定状态
     * @throws ApiException 未配置 503；非微信令牌 400；入参/手机号/密码长度非法 400；
     *                      密码错误或身份归属异常 401；已绑其他手机号、手机号已绑其他微信、壳账号已有意向 409
     */
    public WechatAuthResponse bind(AuthenticatedUser principal, WechatBindRequest request) {
        requireConfigured();
        requireWechat(principal);
        BindCommand command = parseBind(request);
        String targetPhone = resolveTargetPhone(command);
        BoundSession session;
        try {
            session = transactionTemplate.execute(status -> bindTx(principal, command, targetPhone));
        } catch (DuplicateKeyException ex) {
            // 目标账号已有其他微信身份：唯一约束兜底，翻译成 409，不暴露底层异常
            throw WechatMessage.PHONE_BOUND_OTHER_WECHAT.toException();
        }
        if (session.revokePrevious()) {
            // 身份已迁移到目标用户，旧用户那份带 openid 的令牌必须立即失效
            tokenStore.revokeWechatOpenid(session.openid());
        }
        return toResponse(session.user(), session.openid());
    }

    /**
     * 解除当前微信与账号的绑定。
     *
     * <p>只删除「当前 AppID + 当前 openid」这一条身份，并撤销带这枚 openid 的内存令牌；
     * 同一账号的网页版密码令牌继续有效，达到「解绑只影响微信渠道」的语义。
     *
     * @param principal 当前令牌主体，必须是微信渠道
     * @return 解绑后的打通状态（通常 wechatBound 已为 false）
     * @throws ApiException 未配置 503；非微信令牌 400；账号不存在 401
     */
    public WechatLinkResponse unbind(AuthenticatedUser principal) {
        requireConfigured();
        requireWechat(principal);
        PortalUser user = requireUser(principal.userId());
        // 只删当前 AppID + 当前 openid：同一微信在其他小程序/公众号下的身份不受影响
        transactionTemplate.executeWithoutResult(status -> identityMapper.delete(
                Wrappers.<PortalWechatIdentity>lambdaQuery()
                        .eq(PortalWechatIdentity::getAppId, appId())
                        .eq(PortalWechatIdentity::getOpenid, principal.wechatOpenid())));
        // 令牌作废放在事务外：它只动进程内存，不应延长数据库事务
        tokenStore.revokeWechatOpenid(principal.wechatOpenid());
        return linkOf(requireUser(user.getId()));
    }

    /**
     * 查询账号打通状态（只读）。
     *
     * <p>不解绑、不发令牌，手机号渠道的游客令牌也可调用；未配置微信时与登录一样返回 503。
     *
     * @param principal 当前游客主体
     * @return 手机号是否绑定、微信是否绑定、两者是否已打通
     * @throws ApiException 未配置 503；账号不存在 401
     */
    public WechatLinkResponse link(AuthenticatedUser principal) {
        requireConfigured();
        return linkOf(requireUser(principal.userId()));
    }

    /** 按 openid 复用或新建壳用户，并补写 unionid；必须在事务内调用。 */
    private PortalUser persistLogin(JsCodeSession session) {
        PortalWechatIdentity identity = findByOpenid(session.openid());
        if (identity != null) {
            // 老用户：只尝试补 unionid，不改展示名与手机号
            fillUnionid(identity, session.unionid());
            return requireUser(identity.getPortalUserId());
        }
        // 新用户：先建壳账号（无手机号、无密码），展示名用统一的「微信用户」
        PortalUser user = new PortalUser();
        user.setDisplayName(WechatMessage.SHELL_DISPLAY_NAME);
        portalUserMapper.insert(user);
        PortalWechatIdentity row = new PortalWechatIdentity();
        row.setPortalUserId(user.getId());
        row.setAppId(appId());
        row.setOpenid(session.openid());
        row.setUnionid(blankToNull(session.unionid()));
        identityMapper.insert(row);
        return user;
    }

    /** 并发首登撞唯一键后的补偿：重新按 openid 查身份，查不到返回 null 由调用方重抛原异常。 */
    private PortalUser loadAfterConflict(JsCodeSession session) {
        PortalWechatIdentity identity = findByOpenid(session.openid());
        if (identity == null) {
            return null;
        }
        fillUnionid(identity, session.unionid());
        return requireUser(identity.getPortalUserId());
    }

    /**
     * 绑定事务体：校验归属、合并账号、迁移微信身份。
     *
     * <p>分支较多，按目标手机号是否已有账号分两条主路：无账号则给当前壳用户补手机号与密码；
     * 有账号则校验占用/密码/壳用户意向后把身份迁过去，并清理不再被引用的壳用户。
     *
     * @return 目标用户、openid，以及是否需要撤销旧 openid 的令牌
     */
    private BoundSession bindTx(AuthenticatedUser principal, BindCommand command, String targetPhone) {
        PortalUser current = requireUser(principal.userId());
        PortalWechatIdentity identity = findByOpenid(principal.wechatOpenid());
        // 令牌里的 openid 必须确实挂在当前用户下，防止拿着别人的令牌改绑
        if (identity == null || !current.getId().equals(identity.getPortalUserId())) {
            throw WechatMessage.UNAUTHORIZED.toException();
        }
        // 已绑定手机号的账号不允许再换号，只能解绑后重新打通
        if (hasText(current.getPhone()) && !current.getPhone().equals(targetPhone)) {
            throw WechatMessage.ALREADY_OTHER_PHONE.toException();
        }
        if (targetPhone.equals(current.getPhone())) {
            // 重复绑定同一号码：幂等返回；传了密码就顺带校验，不传不改
            if (command.password() != null && !passwordOk(current, command.password())) {
                throw WechatMessage.BAD_PASSWORD.toException();
            }
            return new BoundSession(current, principal.wechatOpenid(), false);
        }
        PortalUser target = findByPhone(targetPhone);
        if (target == null) {
            // 手机号尚未注册：直接把当前壳账号补成正式账号，不新建用户，历史数据留在原 ID 下
            requireNewPassword(command.password());
            current.setPhone(targetPhone);
            current.setPasswordHash(passwordEncoder.encode(command.password()));
            portalUserMapper.updateById(current);
            return new BoundSession(current, principal.wechatOpenid(), false);
        }
        // 目标手机号已有账号：先确认该账号在本 AppID 下没有被其他微信占用
        PortalWechatIdentity occupied = findByUserAndApp(target.getId());
        if (occupied != null && !principal.wechatOpenid().equals(occupied.getOpenid())) {
            throw WechatMessage.PHONE_BOUND_OTHER_WECHAT.toException();
        }
        // 只有传了密码才校验；手机号凭证模式通常不带密码，此时仅凭微信手机号凭证完成合并
        if (command.password() != null && !passwordOk(target, command.password())) {
            throw WechatMessage.BAD_PASSWORD.toException();
        }
        // 壳账号下已有意向时不能合并，否则这些意向会挂在一个将被删除的用户上
        if (intentCount(current.getId()) > 0) {
            throw WechatMessage.SHELL_HAS_INTENTS.toException();
        }
        identity.setPortalUserId(target.getId());
        identityMapper.updateById(identity);
        // 壳账号没有其他微信身份、也没有意向时才物理删除；否则保留，避免误删其他渠道数据
        if (identityCount(current.getId()) == 0 && intentCount(current.getId()) == 0) {
            portalUserMapper.deleteById(current.getId());
        }
        return new BoundSession(target, principal.wechatOpenid(), true);
    }

    /** 新手机号首次设密码：必填，长度 6 到 64。 */
    private void requireNewPassword(String password) {
        if (password == null) {
            throw WechatMessage.PASSWORD_REQUIRED.toException();
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw WechatMessage.PASSWORD_LENGTH.toException();
        }
    }

    /** 解析目标手机号：手填模式直接用；凭证模式调用微信手机号接口并要求返回合法 11 位号码。 */
    private String resolveTargetPhone(BindCommand command) {
        if (!command.phoneCodeMode()) {
            return command.phone();
        }
        WechatMiniappClient.PurePhoneNumber phone = wechatMiniappClient.phoneNumber(command.phoneCode());
        String pure = phone == null ? null : phone.purePhoneNumber();
        if (pure == null || !pure.matches("^1\\d{10}$")) {
            throw WechatMessage.PHONE_INVALID.toException();
        }
        return pure;
    }

    /** 校验绑定入参二选一（凭证或手机号+密码），并归一成内部命令对象。 */
    private BindCommand parseBind(WechatBindRequest request) {
        if (request == null) {
            throw WechatMessage.BODY_INVALID.toException();
        }
        boolean hasCode = request.phoneCode() != null;
        boolean hasPhone = request.phone() != null;
        if (hasCode == hasPhone) {
            throw WechatMessage.BODY_INVALID.toException();
        }
        if (hasCode) {
            String code = request.phoneCode().trim();
            if (code.isEmpty() || code.length() > 128) {
                throw WechatMessage.BODY_INVALID.toException();
            }
            return new BindCommand(code, null, emptyToNull(request.password()));
        }
        String phone = request.phone().trim();
        if (!phone.matches("^1\\d{10}$")) {
            throw WechatMessage.PHONE_INVALID.toException();
        }
        if (request.password() == null) {
            throw WechatMessage.BODY_INVALID.toException();
        }
        if (request.password().length() < PASSWORD_MIN || request.password().length() > PASSWORD_MAX) {
            throw WechatMessage.PASSWORD_LENGTH.toException();
        }
        return new BindCommand(null, phone, request.password());
    }

    /** 校验登录 code：必填、非空白、长度不超过 128。 */
    private String normalizeLoginCode(WechatLoginRequest request) {
        if (request == null || request.code() == null) {
            throw WechatMessage.CODE_REQUIRED.toException();
        }
        String code = request.code().trim();
        if (code.isEmpty()) {
            throw WechatMessage.CODE_REQUIRED.toException();
        }
        if (code.length() > 128) {
            throw WechatMessage.BODY_INVALID.toException();
        }
        return code;
    }

    /** 签发微信渠道令牌并组装响应；未绑定手机号时该字段返回 null，不补空串。 */
    private WechatAuthResponse toResponse(PortalUser user, String openid) {
        PortalUser stored = requireUser(user.getId());
        IssuedToken issued = tokenStore.issue(new AuthenticatedUser(
                stored.getId(),
                UserType.TOURIST,
                null,
                null,
                stored.getDisplayName(),
                blankToNull(stored.getPhone()),
                LoginChannel.WECHAT,
                openid));
        WechatLinkResponse link = linkOf(stored);
        return new WechatAuthResponse(
                issued.token(),
                issued.expiresAt(),
                stored.getId(),
                UserType.TOURIST,
                stored.getDisplayName(),
                link.phone(),
                link.phoneBound(),
                link.wechatBound(),
                link.accountLinked());
    }

    /** 计算打通状态：手机号已绑、密码已设、微信已绑三者同时成立才算 accountLinked。 */
    private WechatLinkResponse linkOf(PortalUser user) {
        boolean phoneBound = hasText(user.getPhone());
        boolean passwordBound = hasText(user.getPasswordHash());
        boolean wechatBound = identityCount(user.getId()) > 0;
        return new WechatLinkResponse(
                user.getId(),
                blankToNull(user.getPhone()),
                phoneBound,
                wechatBound,
                phoneBound && passwordBound && wechatBound);
    }

    /** 只在本地为空、微信本次返回了 unionid 时回填，避免覆盖已有值。 */
    private void fillUnionid(PortalWechatIdentity identity, String unionid) {
        if (!hasText(unionid) || hasText(identity.getUnionid())) {
            return;
        }
        identity.setUnionid(unionid.trim());
        identityMapper.updateById(identity);
    }

    /** 按「当前 AppID + openid」查身份；AppID 参与查询是因为同一微信在不同应用下 openid 不同。 */
    private PortalWechatIdentity findByOpenid(String openid) {
        return identityMapper.selectOne(Wrappers.<PortalWechatIdentity>lambdaQuery()
                .eq(PortalWechatIdentity::getAppId, appId())
                .eq(PortalWechatIdentity::getOpenid, openid));
    }

    /** 查某账号在当前 AppID 下已绑定的微信身份，用于判定「手机号已绑其他微信」。 */
    private PortalWechatIdentity findByUserAndApp(Long userId) {
        return identityMapper.selectOne(Wrappers.<PortalWechatIdentity>lambdaQuery()
                .eq(PortalWechatIdentity::getPortalUserId, userId)
                .eq(PortalWechatIdentity::getAppId, appId()));
    }

    /** 按手机号查游客账号，手机号在门户用户表内唯一。 */
    private PortalUser findByPhone(String phone) {
        return portalUserMapper.selectOne(Wrappers.<PortalUser>lambdaQuery().eq(PortalUser::getPhone, phone));
    }

    /** 统计账号在当前 AppID 下的微信身份数，用于判断壳用户是否还能删除。 */
    private long identityCount(Long userId) {
        Long count = identityMapper.selectCount(Wrappers.<PortalWechatIdentity>lambdaQuery()
                .eq(PortalWechatIdentity::getPortalUserId, userId)
                .eq(PortalWechatIdentity::getAppId, appId()));
        return count == null ? 0 : count;
    }

    /** 统计账号名下的预订意向数；有历史意向的壳用户不允许合并或删除。 */
    private long intentCount(Long userId) {
        Long count = bookingIntentMapper.selectCount(
                Wrappers.<BookingIntent>lambdaQuery().eq(BookingIntent::getUserId, userId));
        return count == null ? 0 : count;
    }

    /** 取账号，不存在统一按未登录 401 处理。 */
    private PortalUser requireUser(Long userId) {
        PortalUser user = portalUserMapper.selectById(userId);
        if (user == null) {
            throw WechatMessage.UNAUTHORIZED.toException();
        }
        return user;
    }

    /** 校验密码；账号未设密码或未传密码时直接判失败，不做空值比较。 */
    private boolean passwordOk(PortalUser user, String password) {
        if (!hasText(user.getPasswordHash()) || password == null) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    /** 未配置 AppID/Secret 时统一 503，避免调用微信接口拿到无意义的 400。 */
    private void requireConfigured() {
        if (!properties.configured()) {
            throw WechatMessage.NOT_CONFIGURED.toException();
        }
    }

    /** 绑定/解绑只接受微信渠道令牌，密码登录的令牌没有 openid，不能走这两条路径。 */
    private void requireWechat(AuthenticatedUser principal) {
        if (principal == null
                || principal.loginChannel() != LoginChannel.WECHAT
                || !hasText(principal.wechatOpenid())) {
            throw WechatMessage.NEED_WECHAT_LOGIN.toException();
        }
    }

    /** 取归一后的 AppID：去首尾空白，null 按空串处理。 */
    private String appId() {
        return properties.normalizedAppId();
    }

    /** 非 null 且含非空白字符。 */
    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** 去空白后为空则返回 null，否则返回 trim 结果。 */
    private static String blankToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /** 空白字符串归一为 null，不做 trim（密码要保留原样）。 */
    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    /**
     * 绑定入参归一结果。
     *
     * @param phoneCode 微信手机号凭证，凭证模式非空
     * @param phone     手填手机号，手填模式非空
     * @param password  可选密码；手机号已存在时用于校验，新手机号时必须用于设密码
     */
    private record BindCommand(String phoneCode, String phone, String password) {
        /** 是否走微信手机号凭证模式；两种模式互斥，由解析阶段保证。 */
        boolean phoneCodeMode() {
            return phoneCode != null;
        }
    }

    /**
     * 绑定事务的返回：目标用户、本次 openid、是否需要撤销旧令牌。
     *
     * @param user           绑定后的目标账号
     * @param openid         当前微信 openid
     * @param revokePrevious 身份是否发生了迁移（迁到已有账号时为 true）
     */
    private record BoundSession(PortalUser user, String openid, boolean revokePrevious) {
    }
}
