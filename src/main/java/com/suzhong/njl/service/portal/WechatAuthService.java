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

    public WechatAuthResponse login(WechatLoginRequest request) {
        requireConfigured();
        String code = normalizeLoginCode(request);
        JsCodeSession session = wechatMiniappClient.codeToSession(code);
        if (session == null || !hasText(session.openid())) {
            throw WechatMessage.UNAVAILABLE.toException();
        }
        PortalUser user;
        try {
            user = transactionTemplate.execute(status -> persistLogin(session));
        } catch (DuplicateKeyException ex) {
            user = transactionTemplate.execute(status -> loadAfterConflict(session));
            if (user == null) {
                throw ex;
            }
        }
        return toResponse(user, session.openid());
    }

    public WechatAuthResponse bind(AuthenticatedUser principal, WechatBindRequest request) {
        requireConfigured();
        requireWechat(principal);
        BindCommand command = parseBind(request);
        String targetPhone = resolveTargetPhone(command);
        BoundSession session;
        try {
            session = transactionTemplate.execute(status -> bindTx(principal, command, targetPhone));
        } catch (DuplicateKeyException ex) {
            throw WechatMessage.PHONE_BOUND_OTHER_WECHAT.toException();
        }
        if (session.revokePrevious()) {
            tokenStore.revokeWechatOpenid(session.openid());
        }
        return toResponse(session.user(), session.openid());
    }

    public WechatLinkResponse unbind(AuthenticatedUser principal) {
        requireConfigured();
        requireWechat(principal);
        PortalUser user = requireUser(principal.userId());
        transactionTemplate.executeWithoutResult(status -> identityMapper.delete(
                Wrappers.<PortalWechatIdentity>lambdaQuery()
                        .eq(PortalWechatIdentity::getAppId, appId())
                        .eq(PortalWechatIdentity::getOpenid, principal.wechatOpenid())));
        tokenStore.revokeWechatOpenid(principal.wechatOpenid());
        return linkOf(requireUser(user.getId()));
    }

    public WechatLinkResponse link(AuthenticatedUser principal) {
        requireConfigured();
        return linkOf(requireUser(principal.userId()));
    }

    private PortalUser persistLogin(JsCodeSession session) {
        PortalWechatIdentity identity = findByOpenid(session.openid());
        if (identity != null) {
            fillUnionid(identity, session.unionid());
            return requireUser(identity.getPortalUserId());
        }
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

    private PortalUser loadAfterConflict(JsCodeSession session) {
        PortalWechatIdentity identity = findByOpenid(session.openid());
        if (identity == null) {
            return null;
        }
        fillUnionid(identity, session.unionid());
        return requireUser(identity.getPortalUserId());
    }

    private BoundSession bindTx(AuthenticatedUser principal, BindCommand command, String targetPhone) {
        PortalUser current = requireUser(principal.userId());
        PortalWechatIdentity identity = findByOpenid(principal.wechatOpenid());
        if (identity == null || !current.getId().equals(identity.getPortalUserId())) {
            throw WechatMessage.UNAUTHORIZED.toException();
        }
        if (hasText(current.getPhone()) && !current.getPhone().equals(targetPhone)) {
            throw WechatMessage.ALREADY_OTHER_PHONE.toException();
        }
        if (targetPhone.equals(current.getPhone())) {
            if (command.password() != null && !passwordOk(current, command.password())) {
                throw WechatMessage.BAD_PASSWORD.toException();
            }
            return new BoundSession(current, principal.wechatOpenid(), false);
        }
        PortalUser target = findByPhone(targetPhone);
        if (target == null) {
            requireNewPassword(command.password());
            current.setPhone(targetPhone);
            current.setPasswordHash(passwordEncoder.encode(command.password()));
            portalUserMapper.updateById(current);
            return new BoundSession(current, principal.wechatOpenid(), false);
        }
        PortalWechatIdentity occupied = findByUserAndApp(target.getId());
        if (occupied != null && !principal.wechatOpenid().equals(occupied.getOpenid())) {
            throw WechatMessage.PHONE_BOUND_OTHER_WECHAT.toException();
        }
        if (command.password() != null && !passwordOk(target, command.password())) {
            throw WechatMessage.BAD_PASSWORD.toException();
        }
        if (intentCount(current.getId()) > 0) {
            throw WechatMessage.SHELL_HAS_INTENTS.toException();
        }
        identity.setPortalUserId(target.getId());
        identityMapper.updateById(identity);
        if (identityCount(current.getId()) == 0 && intentCount(current.getId()) == 0) {
            portalUserMapper.deleteById(current.getId());
        }
        return new BoundSession(target, principal.wechatOpenid(), true);
    }

    private void requireNewPassword(String password) {
        if (password == null) {
            throw WechatMessage.PASSWORD_REQUIRED.toException();
        }
        if (password.length() < PASSWORD_MIN || password.length() > PASSWORD_MAX) {
            throw WechatMessage.PASSWORD_LENGTH.toException();
        }
    }

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

    private void fillUnionid(PortalWechatIdentity identity, String unionid) {
        if (!hasText(unionid) || hasText(identity.getUnionid())) {
            return;
        }
        identity.setUnionid(unionid.trim());
        identityMapper.updateById(identity);
    }

    private PortalWechatIdentity findByOpenid(String openid) {
        return identityMapper.selectOne(Wrappers.<PortalWechatIdentity>lambdaQuery()
                .eq(PortalWechatIdentity::getAppId, appId())
                .eq(PortalWechatIdentity::getOpenid, openid));
    }

    private PortalWechatIdentity findByUserAndApp(Long userId) {
        return identityMapper.selectOne(Wrappers.<PortalWechatIdentity>lambdaQuery()
                .eq(PortalWechatIdentity::getPortalUserId, userId)
                .eq(PortalWechatIdentity::getAppId, appId()));
    }

    private PortalUser findByPhone(String phone) {
        return portalUserMapper.selectOne(Wrappers.<PortalUser>lambdaQuery().eq(PortalUser::getPhone, phone));
    }

    private long identityCount(Long userId) {
        Long count = identityMapper.selectCount(Wrappers.<PortalWechatIdentity>lambdaQuery()
                .eq(PortalWechatIdentity::getPortalUserId, userId)
                .eq(PortalWechatIdentity::getAppId, appId()));
        return count == null ? 0 : count;
    }

    private long intentCount(Long userId) {
        Long count = bookingIntentMapper.selectCount(
                Wrappers.<BookingIntent>lambdaQuery().eq(BookingIntent::getUserId, userId));
        return count == null ? 0 : count;
    }

    private PortalUser requireUser(Long userId) {
        PortalUser user = portalUserMapper.selectById(userId);
        if (user == null) {
            throw WechatMessage.UNAUTHORIZED.toException();
        }
        return user;
    }

    private boolean passwordOk(PortalUser user, String password) {
        if (!hasText(user.getPasswordHash()) || password == null) {
            return false;
        }
        return passwordEncoder.matches(password, user.getPasswordHash());
    }

    private void requireConfigured() {
        if (!properties.configured()) {
            throw WechatMessage.NOT_CONFIGURED.toException();
        }
    }

    private void requireWechat(AuthenticatedUser principal) {
        if (principal == null
                || principal.loginChannel() != LoginChannel.WECHAT
                || !hasText(principal.wechatOpenid())) {
            throw WechatMessage.NEED_WECHAT_LOGIN.toException();
        }
    }

    private String appId() {
        return properties.normalizedAppId();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String blankToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private record BindCommand(String phoneCode, String phone, String password) {
        boolean phoneCodeMode() {
            return phoneCode != null;
        }
    }

    private record BoundSession(PortalUser user, String openid, boolean revokePrevious) {
    }
}
