package org.example.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.config.ConfigManager;

import java.util.Properties;
import java.nio.file.Path;

/**
 * Yahoo SMTP delivery. Configure the Yahoo app password in Settings; never place it in source code.
 */
public final class EmailService {
    private EmailService() {
    }

    public static void sendOtp(String recipient, String otp) {
        send(recipient, "JavaApp ERP verification code", "Your verification code is " + otp + ". It expires in 10 minutes. Do not share this code.");
    }

    public static void send(String recipient, String subject, String body) {
        send(recipient, subject, body, null);
    }

    public static void send(String recipient, String subject, String body, Path attachment) {
        String sender = ConfigManager.get("smtp.email", "");
        String password = ConfigManager.get("smtp.appPassword", "");
        if (sender.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Email is not configured. Open Settings and enter the sending email address and app password.");
        }
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.mail.yahoo.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(sender, password);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(sender));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            message.setSubject(subject);
            if (attachment == null) {
                message.setText(body);
            } else {
                var multipart = new jakarta.mail.internet.MimeMultipart();
                var text = new jakarta.mail.internet.MimeBodyPart();
                text.setText(body);
                multipart.addBodyPart(text);
                var file = new jakarta.mail.internet.MimeBodyPart();
                file.attachFile(attachment.toFile());
                multipart.addBodyPart(file);
                message.setContent(multipart);
            }
            Transport.send(message);
        } catch (Exception ex) {
            throw new IllegalStateException("Email could not be sent. Check the Yahoo email address and app password.", ex);
        }
    }
}
