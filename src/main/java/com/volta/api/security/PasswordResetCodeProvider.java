package com.volta.api.security;

import com.volta.api.config.PasswordResetProperties;
import com.volta.api.database.entity.Users;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

@Component
public class PasswordResetCodeProvider {

    private final SecretKeySpec signingKey;
    private final PasswordResetProperties properties;

    public PasswordResetCodeProvider(@Value("${jwt.key}") String key, PasswordResetProperties properties) {
        this.signingKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.properties = properties;
    }

    public String generate(Users user) {
        return codeFor(user, currentSlot());
    }

    public boolean matches(Users user, String code) {
        long slot = currentSlot();
        return codeFor(user, slot).equals(code) || codeFor(user, slot - 1).equals(code);
    }

    private long currentSlot() {
        long slotMillis = properties.codeValidity().toMillis() / 2;
        return System.currentTimeMillis() / slotMillis;
    }

    private String codeFor(Users user, long slot) {
        String payload = "password-reset|" + user.getId() + "|" + user.getPasswordHash() + "|" + slot;
        int number = ByteBuffer.wrap(sign(payload)).getInt() & Integer.MAX_VALUE;
        return String.format("%04d", number % 10_000);
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to generate password reset code", exception);
        }
    }
}
