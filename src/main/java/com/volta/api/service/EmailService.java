package com.volta.api.service;

import com.volta.api.config.MailProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Slf4j
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

        String safeName = (name != null && !name.isBlank()) ? name.trim() : "colaborador";
        String subject = "VOLTA — Código de recuperação de senha";
        String plainText = buildPlainText(safeName, code, validityMinutes);
        String htmlText = buildHtmlTemplate(safeName, code, validityMinutes);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());
            helper.setFrom(mailProperties.from());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(plainText, htmlText);

            mailSender.send(mimeMessage);
        } catch (Exception ex) {
            log.warn("Falha ao enviar e-mail HTML estilizado, utilizando fallback em texto puro: {}", ex.getMessage());
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailProperties.from());
                message.setTo(to);
                message.setSubject(subject);
                message.setText(plainText);
                mailSender.send(message);
            } catch (Exception fallbackEx) {
                log.error("Falha ao enviar e-mail de recuperação de senha para {}: {}", to, fallbackEx.getMessage());
            }
        }
    }

    private String buildPlainText(String name, String code, long validityMinutes) {
        return "Olá, " + name + "!\n\n"
                + "Recebemos uma solicitação para redefinir a senha da sua conta no VOLTA.\n\n"
                + "Seu código de verificação é: " + code + "\n\n"
                + "Este código expira em " + validityMinutes + " minutos e deixa de valer assim que a senha for alterada.\n\n"
                + "Se você não solicitou a redefinição de senha, ignore este e-mail. Sua conta continua segura.\n\n"
                + "Atenciosamente,\n"
                + "Equipe VOLTA — Gestão Inteligente de Resíduos";
    }

    private String buildHtmlTemplate(String name, String code, long validityMinutes) {
        return "<!DOCTYPE html>"
                + "<html lang=\"pt-BR\">"
                + "<head>"
                + "<meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">"
                + "<title>Recuperação de Senha - VOLTA</title>"
                + "</head>"
                + "<body style=\"margin: 0; padding: 0; background-color: #F4F6F5; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; -webkit-font-smoothing: antialiased;\">"
                + "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #F4F6F5; padding: 40px 16px;\">"
                + "<tr>"
                + "<td align=\"center\">"
                + "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"max-width: 520px; background-color: #FFFFFF; border-radius: 16px; border: 1px solid #E5E9E7; box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04); overflow: hidden;\">"
                + "<tr>"
                + "<td style=\"background: linear-gradient(135deg, #1BA463 0%, #0E7C48 100%); padding: 32px 36px 28px 36px; text-align: left;\">"
                + "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\">"
                + "<tr>"
                + "<td>"
                + "<span style=\"display: inline-block; background-color: rgba(255, 255, 255, 0.2); padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 700; color: #FFFFFF; letter-spacing: 1px;\">VOLTA</span>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style=\"padding-top: 14px;\">"
                + "<h1 style=\"margin: 0; font-size: 24px; font-weight: 700; color: #FFFFFF; line-height: 1.3;\">Recuperação de senha</h1>"
                + "<p style=\"margin: 6px 0 0 0; font-size: 14px; color: rgba(255, 255, 255, 0.9);\">Use o código abaixo para redefinir o seu acesso</p>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style=\"padding: 32px 36px;\">"
                + "<p style=\"margin: 0 0 16px 0; font-size: 15px; color: #1F2937; line-height: 1.5;\">"
                + "Olá, <strong>" + name + "</strong>!"
                + "</p>"
                + "<p style=\"margin: 0 0 24px 0; font-size: 14px; color: #4B5563; line-height: 1.6;\">"
                + "Recebemos um pedido para alterar a senha da sua conta no aplicativo <strong>VOLTA</strong>. Digite o código de 4 dígitos a seguir para continuar o processo:"
                + "</p>"
                + "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"margin-bottom: 24px;\">"
                + "<tr>"
                + "<td align=\"center\" style=\"background-color: #F2FBF6; border: 1.5px dashed #1BA463; border-radius: 12px; padding: 24px 16px;\">"
                + "<span style=\"display: block; font-size: 12px; font-weight: 600; color: #0E7C48; text-transform: uppercase; letter-spacing: 1.5px; margin-bottom: 8px;\">Seu código de verificação</span>"
                + "<div style=\"font-family: 'SF Mono', Monaco, Consolas, 'Courier New', monospace; font-size: 38px; font-weight: 800; color: #0E7C48; letter-spacing: 14px; padding-left: 14px;\">"
                + code
                + "</div>"
                + "<p style=\"margin: 10px 0 0 0; font-size: 13px; color: #166534; font-weight: 500;\">"
                + "⏱️ Válido por " + validityMinutes + " minutos"
                + "</p>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "<table role=\"presentation\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" style=\"background-color: #F9FAFB; border: 1px solid #E5E7EB; border-radius: 8px; margin-bottom: 24px;\">"
                + "<tr>"
                + "<td style=\"padding: 14px 18px;\">"
                + "<p style=\"margin: 0; font-size: 12px; color: #6B7280; line-height: 1.5;\">"
                + "🔒 <strong>Não foi você?</strong> Fique tranquilo! Se não solicitou a troca de senha, basta desconsiderar este e-mail. Nenhuma alteração será efetuada em sua conta."
                + "</p>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "<p style=\"margin: 0; font-size: 13px; color: #9CA3AF; line-height: 1.5;\">"
                + "Por razões de segurança, nunca compartilhe este código com terceiros."
                + "</p>"
                + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style=\"background-color: #F9FAFB; border-top: 1px solid #E5E9E7; padding: 20px 36px; text-align: center;\">"
                + "<p style=\"margin: 0 0 4px 0; font-size: 12px; font-weight: 600; color: #4B5563;\">"
                + "VOLTA — Gestão Inteligente de Resíduos"
                + "</p>"
                + "<p style=\"margin: 0; font-size: 11px; color: #9CA3AF;\">"
                + "Este é um e-mail automático enviado pelo sistema. Por favor, não responda."
                + "</p>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</td>"
                + "</tr>"
                + "</table>"
                + "</body>"
                + "</html>";
    }
}