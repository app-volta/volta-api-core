package com.volta.api;

import com.volta.api.config.AdminProperties;
import com.volta.api.config.MailProperties;
import com.volta.api.config.PasswordResetProperties;
import com.volta.api.config.SwaggerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import java.util.Locale;
import java.util.TimeZone;

@SpringBootApplication
@EnableConfigurationProperties({
        AdminProperties.class,
        SwaggerProperties.class,
        MailProperties.class,
        PasswordResetProperties.class
})
public class VoltaApiApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.ENGLISH);
        SpringApplication.run(VoltaApiApplication.class, args);
    }

}
