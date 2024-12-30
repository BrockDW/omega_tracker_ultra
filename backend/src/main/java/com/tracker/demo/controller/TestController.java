package com.tracker.demo.controller;

import com.tracker.demo.constants.Constants;
import com.tracker.demo.service.MailgunEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private MailgunEmailService mailgunEmailService;

    @GetMapping("/test-email")
    public String sendTestEmail() {
        String subject = "Manual Email Test";
        String body = "Hello! This is a test email from our Spring Boot app.";

        mailgunEmailService.sendSimpleEmail(Constants.TARGET_EMAIL, subject, body);
        return "Test email sent to " + Constants.TARGET_EMAIL;
    }
}
