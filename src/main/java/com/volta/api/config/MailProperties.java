package com.volta.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mail")
public record MailProperties(
        String from
) {

    public boolean isConfigured() {
        return from != null && !from.isBlank();
    }
}
