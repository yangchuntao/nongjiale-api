package com.suzhong.njl.service.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 手机号脱敏工具。
 *
 * <p>用在商家/运营意向只读接口与注册时的默认展示名：明文手机号只允许运营端详情使用，
 * 其他出口一律走这里。
 */
public final class PhoneMask {

    /** TECH：商家意向联系电话 (\\d{3})\\d{4}(\\d{4}) → $1****$2。 */
    private static final Pattern CONTACT = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");

    private PhoneMask() {
    }

    /**
     * 保留前 3 位与后 4 位，中间统一替换为 4 个星号。
     *
     * <p>不足 7 位时无法凑出「前 3 + 后 4」，原样返回，避免越界或截出负数区间。
     *
     * @param phone 原始手机号，可为 null
     * @return 脱敏后的号码；入参为 null 或过短时原样返回
     */
    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 按商家意向的联系电话格式脱敏。
     *
     * <p>只有整串恰好是 11 位数字时才按 3-4-4 分段替换；带区号、分机等脏数据回退到
     * {@link #mask(String)}，保证不会把非手机号文本截坏。
     *
     * @param phone 联系电话，可为 null
     * @return 脱敏后的文本；null 原样返回
     */
    public static String maskContact(String phone) {
        if (phone == null) {
            return null;
        }
        Matcher matcher = CONTACT.matcher(phone);
        if (matcher.matches()) {
            return matcher.group(1) + "****" + matcher.group(2);
        }
        return mask(phone);
    }
}
