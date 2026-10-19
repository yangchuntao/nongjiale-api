package com.suzhong.njl.service.portal;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

/**
 * 意向单号生成器（门户端）。
 *
 * <p>格式：{@code NJL} + {@code yyyyMMdd}（应用时区）+ 4 字节随机数的 8 位大写十六进制，
 * 例如 {@code NJL20261019A1B2C3D4}。用 {@link SecureRandom} 而非自增序列，避免暴露单量。
 *
 * <p>不保证绝对唯一：碰撞由 {@code booking_intent.intent_no} 唯一索引拦截，调用方重试。
 */
@Component
public class IntentNumbers {

    /** 日期段格式，BASIC_ISO_DATE 即 {@code yyyyMMdd}，取自应用时钟避免时区漂移。 */
    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final Clock applicationClock;
    private final SecureRandom random = new SecureRandom();

    public IntentNumbers(Clock applicationClock) {
        this.applicationClock = applicationClock;
    }

    /**
     * 生成一个新的意向单号，不做查库去重。
     *
     * @return 形如 {@code NJL20261019A1B2C3D4} 的单号
     */
    public String next() {
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        return "NJL" + LocalDate.now(applicationClock).format(DAY) + java.util.HexFormat.of().formatHex(bytes).toUpperCase();
    }
}
