package com.volta.api.service;

import com.volta.api.config.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;

    public void sendPasswordResetCode(String to, String name, String code, long validityMinutes) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            return;
        }
        if (!mailProperties.isConfigured()) {
            return;
        }

        String greeting = "Olá, " + name + "!\n\n";
        String codeLine = "Seu código de recuperação de senha é: " + code + "\n";
        String validityLine = "Ele expira em até " + validityMinutes + " minutos e deixa de valer assim que a senha for trocada.\n\n";
        String ignoreLine = "Se você não pediu a recuperação, ignore este e-mail.";
        String body = greeting + codeLine + validityLine + ignoreLine;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(to);
        message.setSubject("Volta - código de recuperação de senha");
        message.setText(body);

        mailSender.send(message);
    }
}
