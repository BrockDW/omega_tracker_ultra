package com.tracker.demo.controller;

import com.tracker.demo.constants.Constants;
import com.tracker.demo.dto.KeyBrPracticeResult;
import com.tracker.demo.service.ChatGPTLoginService;
import com.tracker.demo.service.KeybrScraperServiceV2;
import com.tracker.demo.service.MailgunEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @Autowired
    private MailgunEmailService mailgunEmailService;

    @Autowired
    private ChatGPTLoginService chatGPTLoginService;

//    @Autowired
//    private KeybrScraperService keybrScraperService;

    @Autowired
    private KeybrScraperServiceV2 keybrScraperServiceV2;

    @GetMapping("/scrape-chatgpt")
    public String scrapeChatGPT() {
        // Attempt to log in and get the page source
        String pageHtml = chatGPTLoginService.loginAndGetPageContent();
        return pageHtml;
    }

    @GetMapping("/test-email")
    public String sendTestEmail() {
        String subject = "Manual Email Test";
        String body = "Hello! This is a test email from our Spring Boot app.";

        mailgunEmailService.sendSimpleEmail(Constants.TARGET_EMAIL, subject, body);
        return "Test email sent to " + Constants.TARGET_EMAIL;
    }

//    @GetMapping("/practice-time")
//    public String getPracticeTime() {
//        return keybrScraperService.getPracticeTimeWithSession();
//    }

    @GetMapping("/practice-time-v2")
    public KeyBrPracticeResult getPracticeTimeV2() {
        return keybrScraperServiceV2.getPracticeTimeWithSession();
    }
}
