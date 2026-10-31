package com.volta.api.config;

import com.volta.api.handler.RestAccessDeniedHandler;
import com.volta.api.handler.RestAuthenticationEntryPoint;
import com.volta.api.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.UUID;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(
            HttpSecurity http,
            SwaggerProperties swaggerProperties,
            PasswordEncoder passwordEncoder
    ) throws Exception {
        UserDetails swaggerUser = buildSwaggerUser(swaggerProperties, passwordEncoder);

        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(swaggerUser));
        provider.setPasswordEncoder(passwordEncoder);

        return http
                .securityMatcher(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/api-docs/**"
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationManager(new ProviderManager(provider))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .build();
    }

    private UserDetails buildSwaggerUser(SwaggerProperties swaggerProperties, PasswordEncoder passwordEncoder) {
        if (swaggerProperties.isConfigured()) {
            return User.withUsername(swaggerProperties.username())
                    .password(passwordEncoder.encode(swaggerProperties.password()))
                    .build();
        }

        return User.withUsername(UUID.randomUUID().toString())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers(
                                        "/auth/**",
                                        "/actuator/health/**"
                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
