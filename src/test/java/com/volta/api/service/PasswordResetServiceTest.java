package com.volta.api.service;

import com.volta.api.config.PasswordResetProperties;
import com.volta.api.database.entity.Users;
import com.volta.api.database.repository.UserRepository;
import com.volta.api.dto.auth.ForgotPasswordRequestDTO;
import com.volta.api.dto.auth.MessageResponseDTO;
import com.volta.api.dto.auth.ResetPasswordRequestDTO;
import com.volta.api.dto.auth.VerifyCodeRequestDTO;
import com.volta.api.exception.InvalidRequestException;
import com.volta.api.security.PasswordResetCodeProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService")
class PasswordResetServiceTest {

    private final PasswordResetProperties properties = new PasswordResetProperties(Duration.ofMinutes(10));

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    private PasswordResetCodeProvider codeProvider;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        codeProvider = new PasswordResetCodeProvider("chave-somente-para-testes-com-mais-de-32-bytes", properties);
        service = new PasswordResetService(userRepository, passwordEncoder, emailService, codeProvider, properties);
    }

    private Users user() {
        return Users.builder()
                .id(UUID.randomUUID()).name("Ana").email("ana@volta.com")
                .passwordHash("old-hash").firstLogin(true).build();
    }

    private String wrongCodeFor(Users user) {
        String code = codeProvider.generate(user);
        return String.format("%04d", (Integer.parseInt(code) + 1) % 10_000);
    }

    @Nested
    @DisplayName("requestCode")
    class RequestCode {

        @Test
        @DisplayName("e-mail inexistente responde a mensagem padrão sem enviar")
        void requestCode_unknownEmail_returnsMessageWithoutSending() {
            when(userRepository.findByEmail("ghost@volta.com")).thenReturn(Optional.empty());

            MessageResponseDTO response = service.requestCode(new ForgotPasswordRequestDTO("ghost@volta.com"));

            assertThat(response.message()).isEqualTo("If the email exists, a code was sent");
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("e-mail existente envia o código por e-mail")
        void requestCode_existingEmail_sendsCodeByEmail() {
            Users user = user();
            when(userRepository.findByEmail("ana@volta.com")).thenReturn(Optional.of(user));

            MessageResponseDTO response = service.requestCode(new ForgotPasswordRequestDTO("ana@volta.com"));

            assertThat(response.message()).isEqualTo("If the email exists, a code was sent");
            ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
            verify(emailService).sendPasswordResetCode(eq("ana@volta.com"), eq("Ana"), code.capture(), eq(10L));
            assertThat(code.getValue()).matches("\\d{4}");
            assertThat(codeProvider.matches(user, code.getValue())).isTrue();
        }
    }

    @Nested
    @DisplayName("verifyCode")
    class VerifyCode {

        @Test
        @DisplayName("e-mail inexistente lança código inválido")
        void verifyCode_unknownEmail_throwsInvalidCode() {
            when(userRepository.findByEmail("ghost@volta.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyCode(new VerifyCodeRequestDTO("ghost@volta.com", "1234")))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage("Invalid or expired code");
        }

        @Test
        @DisplayName("código errado lança código inválido")
        void verifyCode_wrongCode_throwsInvalidCode() {
            Users user = user();
            when(userRepository.findByEmail("ana@volta.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.verifyCode(new VerifyCodeRequestDTO("ana@volta.com", wrongCodeFor(user))))
                    .isInstanceOf(InvalidRequestException.class)
                    .hasMessage("Invalid or expired code");
        }

        @Test
        @DisplayName("código correto passa sem alterar o usuário")
        void verifyCode_correctCode_passesWithoutSaving() {
            Users user = user();
            when(userRepository.findByEmail("ana@volta.com")).thenReturn(Optional.of(user));

            service.verifyCode(new VerifyCodeRequestDTO("ana@volta.com", codeProvider.generate(user)));

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("código correto troca a senha e desmarca firstLogin")
        void resetPassword_correctCode_changesPasswordAndClearsFirstLogin() {
            Users user = user();
            when(userRepository.findByEmail("ana@volta.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("new-hash");

            service.resetPassword(new ResetPasswordRequestDTO("ana@volta.com", codeProvider.generate(user), "novaSenha123"));

            assertThat(user.getPasswordHash()).isEqualTo("new-hash");
            assertThat(user.getFirstLogin()).isFalse();
            verify(userRepository).save(user);
        }

        @Test
        @DisplayName("o mesmo código não funciona uma segunda vez")
        void resetPassword_sameCodeTwice_failsTheSecondTime() {
            Users user = user();
            when(userRepository.findByEmail("ana@volta.com")).thenReturn(Optional.of(user));
            when(passwordEncoder.encode("novaSenha123")).thenReturn("new-hash");
            String code = codeProvider.generate(user);

            service.resetPassword(new ResetPasswordRequestDTO("ana@volta.com", code, "novaSenha123"));

            assertThatThrownBy(() -> service.resetPassword(
                    new ResetPasswordRequestDTO("ana@volta.com", code, "outraSenha123")))
                    .isInstanceOf(InvalidRequestException.class);

            assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        }

        @Test
        @DisplayName("código errado não troca a senha")
        void resetPassword_wrongCode_doesNotChangePassword() {
            Users user = user();
            when(userRepository.findByEmail("ana@volta.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.resetPassword(
                    new ResetPasswordRequestDTO("ana@volta.com", wrongCodeFor(user), "novaSenha123")))
                    .isInstanceOf(InvalidRequestException.class);

            assertThat(user.getPasswordHash()).isEqualTo("old-hash");
            verify(userRepository, never()).save(any());
        }
    }
}
