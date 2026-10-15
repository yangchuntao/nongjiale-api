package com.suzhong.njl.service.portal;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

@Component
public class IntentNumbers {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final Clock applicationClock;
    private final SecureRandom random = new SecureRandom();

    public IntentNumbers(Clock applicationClock) {
        this.applicationClock = applicationClock;
    }

    public String next() {
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        return "NJL" + LocalDate.now(applicationClock).format(DAY) + java.util.HexFormat.of().formatHex(bytes).toUpperCase();
    }
}
