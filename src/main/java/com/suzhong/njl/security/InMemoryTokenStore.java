package com.suzhong.njl.security;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import com.suzhong.njl.config.AppProperties;
import com.suzhong.njl.domain.enums.UserType;

/**
 * 进程内 Bearer 令牌存储，一期唯一的 {@link TokenStore} 实现。
 *
 * <p>职责：签发 32 字节随机数（十六进制 64 字符）作为令牌，保存「令牌 → 登录主体 + 过期时间」，
 * 并在解析时做过期判断。令牌不是 JWT：没有签名与自描述载荷，所有状态都在本进程内存里。
 *
 * <p>生命周期与进程一致：应用重启即全部失效，用户需要重新登录；多实例部署时各实例的令牌互不通用。
 * 因而不写 Redis、不落库，也不参与任何持久化。
 *
 * <p>并发与清理：
 * <ul>
 *   <li>使用 {@link ConcurrentHashMap}，签发、解析、批量作废可并发执行，无需额外加锁；</li>
 *   <li>没有定时清理线程，过期记录采用「惰性删除」——只有再次拿同一令牌调用 {@link #resolve(String)} 时才移除；</li>
 *   <li>已过期但再未被访问的记录会一直占用内存，令牌量级小且 TTL 默认 12 小时，暂不引入清扫任务。</li>
 * </ul>
 *
 * <p>关键限制：{@link #revokeWechatOpenid(String)} 按 openid 作废该微信的全部令牌（解绑或身份迁移时调用），
 * 但不会影响网页版密码登录的令牌（其 openid 为空）。
 */
@Component
public class InMemoryTokenStore implements TokenStore {

    private final AppProperties appProperties;
    private final Clock applicationClock;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, TokenRecord> tokens = new ConcurrentHashMap<>();

    public InMemoryTokenStore(AppProperties appProperties, Clock applicationClock) {
        this.appProperties = appProperties;
        this.applicationClock = applicationClock;
    }

    /**
     * 签发新令牌。
     *
     * <p>令牌由 {@link SecureRandom} 生成 32 字节后转十六进制，碰撞概率可忽略；
     * 有效期取 {@code app.security.token-ttl}（默认 12 小时），以应用时钟为基准计算绝对过期时刻。
     * 同一用户重复登录会得到多枚并存的有效令牌，本方法不会作废旧令牌。
     *
     * @param user 登录主体快照，不能为空；调用方保证字段完整
     * @return 令牌原文与绝对过期时刻，令牌不含 {@code Bearer } 前缀
     */
    @Override
    public IssuedToken issue(AuthenticatedUser user) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String token = java.util.HexFormat.of().formatHex(bytes);
        Instant expiresAt = Instant.now(applicationClock).plus(appProperties.getSecurity().getTokenTtl());
        tokens.put(token, new TokenRecord(user, expiresAt));
        return new IssuedToken(token, expiresAt);
    }

    /**
     * 解析令牌为登录主体。
     *
     * <p>空值、空白、未知令牌、已过期令牌都返回空 {@link Optional}，不抛异常——调用方据此按未登录处理。
     * 过期记录在本次调用时顺手删除；使用 {@code remove(key, value)} 避免误删并发签发的新记录。
     *
     * @param token 令牌原文，可为 {@code null} 或空白
     * @return 命中且未过期时返回主体；否则为空
     */
    @Override
    public Optional<AuthenticatedUser> resolve(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        TokenRecord record = tokens.get(token);
        if (record == null) {
            return Optional.empty();
        }
        // 过期时刻等于当前时刻即视为过期（要求严格晚于当前时间）
        if (!record.expiresAt().isAfter(Instant.now(applicationClock))) {
            tokens.remove(token, record);
            return Optional.empty();
        }
        return Optional.of(record.user());
    }

    /**
     * 作废某个商家主体名下的全部令牌。
     *
     * <p>用于运营停用商家或商家改密后强制下线。只处理 {@link UserType#MERCHANT} 且
     * {@code merchantOrgId} 相同的记录，游客与运营令牌不受影响。
     *
     * @param merchantOrgId 商家主体 ID；为 {@code null} 时直接返回，避免误删所有无归属令牌
     */
    @Override
    public void revokeMerchantOrg(Long merchantOrgId) {
        if (merchantOrgId == null) {
            return;
        }
        // 遍历删除而非维护倒排索引：令牌数量小，实现简单且无需考虑索引一致性
        tokens.entrySet().removeIf(entry -> {
            AuthenticatedUser user = entry.getValue().user();
            return user.userType() == UserType.MERCHANT && merchantOrgId.equals(user.merchantOrgId());
        });
    }

    /**
     * 作废带指定微信 openid 的全部令牌。
     *
     * <p>微信解绑或微信身份迁移到其他账号时调用。网页版密码登录的令牌 openid 为空，不会被误伤，
     * 因此解绑不会把网页端会话一起踢掉。
     *
     * @param openid 微信 openid；为 {@code null} 或空白时直接返回
     */
    @Override
    public void revokeWechatOpenid(String openid) {
        if (openid == null || openid.isBlank()) {
            return;
        }
        tokens.entrySet().removeIf(entry -> openid.equals(entry.getValue().user().wechatOpenid()));
    }

    /**
     * 内存中的一条令牌记录：主体快照 + 绝对过期时刻。
     *
     * <p>record 不可变，签发后只读，可被多线程安全共享；更新令牌等价于写入新记录。
     *
     * @param user      令牌对应的登录主体快照（角色、用户 ID、openid 等）
     * @param expiresAt 绝对过期时刻，由签发时的 TTL 计算得出；读取时与当前时间比较判定失效
     */
    private record TokenRecord(AuthenticatedUser user, Instant expiresAt) {
    }
}
