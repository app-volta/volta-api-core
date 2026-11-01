package com.volta.api.security;

import com.volta.api.config.PasswordResetProperties;
import com.volta.api.database.entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;

@Component
public class PasswordResetCodeProvider {

    private final SecretKeySpec signingKey;
    private final PasswordResetProperties properties;
    private final Clock clock;

    @Autowired
    public PasswordResetCodeProvider(@Value("${jwt.key}") String key, PasswordResetProperties properties) {
        this(key, properties, Clock.systemUTC());
    }

    PasswordResetCodeProvider(String key, PasswordResetProperties properties, Clock clock) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        this.signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        this.properties = properties;
        this.clock = clock;
    }

    public String generate(Users user) {
        long slot = currentSlot();
        String code = codeFor(user, slot);
        return code;
    }

    public boolean matches(Users user, String code) {
        long slot = currentSlot();
        String currentCode = codeFor(user, slot);
        String previousCode = codeFor(user, slot - 1);

        boolean matchesCurrent = isEqual(currentCode, code);
        boolean matchesPrevious = isEqual(previousCode, code);

        return matchesCurrent || matchesPrevious;
    }

    private long currentSlot() {
        long validityMillis = properties.codeValidity().toMillis();
        long slotMillis = validityMillis / 2;
        long nowMillis = clock.millis();
        return nowMillis / slotMillis;
    }

    private String codeFor(Users user, long slot) {
        String payload = "password-reset|" + user.getId() + "|" + user.getPasswordHash() + "|" + slot;
        byte[] signature = sign(payload);
        int number = ByteBuffer.wrap(signature).getInt() & Integer.MAX_VALUE;
        int fourDigits = number % 10_000;
        return String.format("%04d", fourDigits);
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            return mac.doFinal(payloadBytes);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to generate password reset code", exception);
        }
    }

    private boolean isEqual(String expected, String actual) {
        byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
        byte[] actualBytes = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expectedBytes, actualBytes);
    }
}
