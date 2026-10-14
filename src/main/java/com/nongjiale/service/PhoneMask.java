package com.nongjiale.service;

public final class PhoneMask {
    private PhoneMask() {}

    /** 保留前 3 后 4，例如 138****5678。 */
    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) return "****";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
