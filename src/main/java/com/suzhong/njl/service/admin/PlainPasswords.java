package com.suzhong.njl.service.admin;

import java.security.SecureRandom;

/**
 * 商家账号明文密码工具（运营端内部使用）。
 *
 * <p>职责：入驻通过、重置密码时决定本次使用的明文密码。运营传了就用传入值，未传或空白则生成
 * 10 位随机密码。本类只产出明文，BCrypt 哈希由调用方完成；明文只在当次响应返回一次，
 * 不落库、不写日志。
 *
 * <p>随机字符集排除了易混淆的 I、O、0、1，降低运营转述密码时的抄错概率。
 */
final class PlainPasswords {

    /** 随机密码字符集：去掉 I/O/0/1 等易混淆字符。 */
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PlainPasswords() {
    }

    /**
     * 解析本次使用的明文密码：传入非空白值原样返回，否则生成随机密码。
     *
     * <p>长度与格式校验由请求 DTO 完成，本方法不再二次校验。
     *
     * @param requested 运营指定的明文，可为 null
     * @return 最终明文，非 null
     */
    static String resolve(String requested) {
        if (requested == null || requested.isBlank()) {
            return generate();
        }
        return requested;
    }

    /** 用 SecureRandom 生成 10 位随机密码。 */
    private static String generate() {
        StringBuilder builder = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
