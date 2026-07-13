package org.example.service;

import java.security.SecureRandom;
import java.time.Instant;

public final class OtpService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static String code;
    private static Instant expires;

    private OtpService() {
    }

    public static String issue() {
        code = String.format("%06d", RANDOM.nextInt(1_000_000));
        expires = Instant.now().plusSeconds(600);
        return code;
    }

    public static boolean verify(String candidate) {
        boolean valid = code != null && expires != null && Instant.now().isBefore(expires) && code.equals(candidate == null ? "" : candidate.trim());
        if (valid) {
            code = null;
            expires = null;
        }
        return valid;
    }
}
