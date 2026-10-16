package com.suzhong.njl.service.admin;

import java.security.SecureRandom;

final class PlainPasswords {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PlainPasswords() {
    }

    static String resolve(String requested) {
        if (requested == null || requested.isBlank()) {
            return generate();
        }
        return requested;
    }

    private static String generate() {
        StringBuilder builder = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
