package com.tracker.demo.service;

import com.tracker.demo.constants.Constants;
import com.tracker.demo.dto.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.util.List;

@Service
public class DailyEmailReminderService {
    @Autowired
    private DailyTaskService dailyTaskService;

    @Autowired
    private MailgunEmailService mailgunEmailService;

    @Scheduled(cron = "0 1 0 * * ?")
    public void midnightReminder() {
        // send "create or update today's note" email
        String subject = "Time to start today's note!";
        String body = "Hello,\nPlease create or update your note for today.\n--Your Tracker";
        mailgunEmailService.sendSimpleEmail(Constants.TARGET_EMAIL, subject, body);
    }

    @Scheduled(cron = "0 0 8 * * ?")
    public void morningReminder() {
        List<Task> tasks = dailyTaskService.fetchMarkdownLocalDate(LocalDate.now());
        if (!CollectionUtils.isEmpty(tasks)) {
            // fetch tasks, highlight incomplete, etc.
            String body = buildTaskEmailBody(tasks);
            String subject = "Your tasks for today";
            mailgunEmailService.sendSimpleEmail(Constants.TARGET_EMAIL, subject, body);
        } else {
            // note not found => remind again
            String subject = "Reminder: Please create today's note!";
            String body = "Hello,\nWe can't find today's note. Please create it.\n--Your Tracker";
            mailgunEmailService.sendSimpleEmail(Constants.TARGET_EMAIL, subject, body);
        }
    }

    private String buildTaskEmailBody(List<Task> tasks) {
        // e.g. "Today's Tasks:\n - [ ] task1\n - [x] task2\n etc."
        // or do some formatting logic
        StringBuilder sb = new StringBuilder("Hello,\nHere are today's tasks:\n");
        for (Task t : tasks) {
            sb.append(t.isCompleted() ? " [x] " : " [ ] ")
                    .append(t.getDescription())
                    .append("\n");
        }
        sb.append("\n--Your Tracker");
        return sb.toString();
    }
}

