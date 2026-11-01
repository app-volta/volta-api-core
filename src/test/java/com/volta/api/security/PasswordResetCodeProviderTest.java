package com.volta.api.security;

import com.volta.api.config.PasswordResetProperties;
import com.volta.api.database.entity.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordResetCodeProvider")
class PasswordResetCodeProviderTest {

    private final PasswordResetProperties properties = new PasswordResetProperties(Duration.ofMinutes(10));

    private final PasswordResetCodeProvider provider = new PasswordResetCodeProvider(
            "chave-somente-para-testes-com-mais-de-32-bytes", properties);

    private final Users user = Users.builder()
            .id(UUID.randomUUID()).email("ana@volta.com").passwordHash("hash-1").build();

    @Test
    @DisplayName("gera código de 4 dígitos")
    void generate_returnsFourDigits() {
        assertThat(provider.generate(user)).matches("\\d{4}");
    }

    @Test
    @DisplayName("código gerado é aceito")
    void matches_freshCode_returnsTrue() {
        assertThat(provider.matches(user, provider.generate(user))).isTrue();
    }

    @Test
    @DisplayName("código errado é recusado")
    void matches_wrongCode_returnsFalse() {
        String code = provider.generate(user);
        String wrong = String.format("%04d", (Integer.parseInt(code) + 1) % 10_000);

        assertThat(provider.matches(user, wrong)).isFalse();
    }

    @Test
    @DisplayName("código deixa de valer depois que a senha muda")
    void matches_afterPasswordChange_returnsFalse() {
        String code = provider.generate(user);

        user.setPasswordHash("hash-2");

        assertThat(provider.matches(user, code)).isFalse();
    }

    @Test
    @DisplayName("cada usuário valida apenas o próprio código")
    void matches_otherUser_validatesOnlyOwnCode() {
        Users other = Users.builder().id(UUID.randomUUID()).email("bia@volta.com").passwordHash("hash-1").build();

        String code = provider.generate(user);
        String otherCode = provider.generate(other);

        assertThat(provider.matches(other, otherCode)).isTrue();
        assertThat(code.equals(otherCode) || !provider.matches(other, code)).isTrue();
    }
}
