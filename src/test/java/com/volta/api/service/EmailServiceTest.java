package com.volta.api.service;

import com.volta.api.config.MailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailService")
class EmailServiceTest {

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender javaMailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MailProperties properties = new MailProperties("no-reply@volta.com");
        emailService = new EmailService(mailSenderProvider, properties);
    }

    @Test
    @DisplayName("envia e-mail HTML estilizado com sucesso quando configurado")
    void sendPasswordResetCode_success() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetCode("ana@volta.com", "Ana Silva", "4821", 10L);

        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("não envia quando mailSender não estiver disponível")
    void sendPasswordResetCode_mailSenderNull_doesNothing() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);

        emailService.sendPasswordResetCode("ana@volta.com", "Ana", "4821", 10L);

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("não envia quando mail.from não estiver configurado")
    void sendPasswordResetCode_fromNotConfigured_doesNothing() {
        MailProperties emptyProps = new MailProperties("");
        EmailService unconfiguredService = new EmailService(mailSenderProvider, emptyProps);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);

        unconfiguredService.sendPasswordResetCode("ana@volta.com", "Ana", "4821", 10L);

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("aciona fallback em texto puro quando createMimeMessage falhar")
    void sendPasswordResetCode_fallbackToSimpleMailMessage() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        when(javaMailSender.createMimeMessage()).thenThrow(new RuntimeException("Mime error"));

        emailService.sendPasswordResetCode("ana@volta.com", "Ana", "4821", 10L);

        verify(javaMailSender).send(any(SimpleMailMessage.class));
    }
}