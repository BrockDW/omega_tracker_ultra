package com.tracker.demo.service;

import com.tracker.demo.constants.Constants;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailgunEmailService {
    private final JavaMailSender mailSender;

    public MailgunEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(Constants.EMAIL_SUBJECT + " <" + Constants.SOURCE_EMAIL + ">");
        // or omit setFrom() if mailgun uses domain default
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);

        mailSender.send(message);
    }

    // Example for HTML email (using MimeMessageHelper):
    public void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setFrom(Constants.EMAIL_SUBJECT + " <" + Constants.SOURCE_EMAIL + ">");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = isHtml
        mailSender.send(mimeMessage);
    }
}
