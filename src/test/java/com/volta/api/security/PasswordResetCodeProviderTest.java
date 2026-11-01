package com.volta.api.security;

import com.volta.api.config.PasswordResetProperties;
import com.volta.api.database.entity.Users;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordResetCodeProvider")
class PasswordResetCodeProviderTest {

    private final String key = "chave-somente-para-testes-com-mais-de-32-bytes";

    private final Instant start = Instant.parse("2026-11-01T12:00:00Z");

    private final Duration codeValidity = Duration.ofMinutes(10);

    private final PasswordResetProperties properties = new PasswordResetProperties(codeValidity);

    private final Users user = Users.builder()
            .id(UUID.randomUUID()).email("ana@volta.com").passwordHash("hash-1").build();

    private PasswordResetCodeProvider providerAt(Instant instant) {
        return new PasswordResetCodeProvider(key, properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("gera código de 4 dígitos")
    void generate_returnsFourDigits() {
        assertThat(providerAt(start).generate(user)).matches("\\d{4}");
    }

    @Test
    @DisplayName("gera o mesmo código dentro da mesma janela")
    void generate_sameSlot_returnsSameCode() {
        String first = providerAt(start).generate(user);
        String second = providerAt(start.plusSeconds(60)).generate(user);

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("código gerado é aceito no mesmo instante")
    void matches_freshCode_returnsTrue() {
        PasswordResetCodeProvider provider = providerAt(start);

        assertThat(provider.matches(user, provider.generate(user))).isTrue();
    }

    @Test
    @DisplayName("código continua válido na janela seguinte")
    void matches_nextSlot_returnsTrue() {
        String code = providerAt(start).generate(user);

        assertThat(providerAt(start.plus(codeValidity.dividedBy(2))).matches(user, code)).isTrue();
    }

    @Test
    @DisplayName("código expira depois de duas janelas")
    void matches_afterMaxValidity_returnsFalse() {
        String code = providerAt(start).generate(user);

        assertThat(providerAt(start.plus(codeValidity)).matches(user, code)).isFalse();
    }

    @Test
    @DisplayName("código errado é recusado")
    void matches_wrongCode_returnsFalse() {
        PasswordResetCodeProvider provider = providerAt(start);
        String code = provider.generate(user);
        String wrong = String.format("%04d", (Integer.parseInt(code) + 1) % 10_000);

        assertThat(provider.matches(user, wrong)).isFalse();
    }

    @Test
    @DisplayName("código deixa de valer depois que a senha muda")
    void matches_afterPasswordChange_returnsFalse() {
        PasswordResetCodeProvider provider = providerAt(start);
        String code = provider.generate(user);

        user.setPasswordHash("hash-2");

        assertThat(provider.matches(user, code)).isFalse();
    }

    @Test
    @DisplayName("cada usuário valida apenas o próprio código")
    void matches_otherUser_validatesOnlyOwnCode() {
        PasswordResetCodeProvider provider = providerAt(start);
        Users other = Users.builder().id(UUID.randomUUID()).email("bia@volta.com").passwordHash("hash-1").build();

        String code = provider.generate(user);
        String otherCode = provider.generate(other);

        assertThat(provider.matches(other, otherCode)).isTrue();
        assertThat(code.equals(otherCode) || !provider.matches(other, code)).isTrue();
    }
}
