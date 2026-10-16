package com.suzhong.njl.service.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PhoneMask {

    /** TECH：商家意向联系电话 (\\d{3})\\d{4}(\\d{4}) → $1****$2。 */
    private static final Pattern CONTACT = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");

    private PhoneMask() {
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

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
