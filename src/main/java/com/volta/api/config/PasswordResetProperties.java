package com.volta.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "password-reset")
public record PasswordResetProperties(
        Duration codeValidity
) {
}
