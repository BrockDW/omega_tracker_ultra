package com.tracker.demo.service;

import com.tracker.demo.obj.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DailyTaskService {

    private final GitHubReadService gitHubReadService;
    private static final Pattern CHECKBOX_PATTERN = Pattern.compile("^- \\[([ xX])\\] (.*)$");

    public DailyTaskService(GitHubReadService GitHubReadService) {
        this.gitHubReadService = GitHubReadService;
    }

    public List<Task> getTodayTasks() {
        String mdContent = gitHubReadService.fetchMarkdownForLocalDate(LocalDate.now());
        if (mdContent == null || mdContent.isEmpty()) {
            return Collections.emptyList();
        }
        return parseMarkdown(mdContent);
    }

    private List<Task> parseMarkdown(String markdown) {
        List<Task> tasks = new ArrayList<>();
        String[] lines = markdown.split("\r?\n"); // split on newlines

        for (String line : lines) {
            Matcher matcher = CHECKBOX_PATTERN.matcher(line);
            if (matcher.matches()) {
                String checkMark = matcher.group(1).trim();
                String description = matcher.group(2).trim();
                boolean completed = checkMark.equalsIgnoreCase("x");
                tasks.add(new Task(description, completed));
            }
        }
        return tasks;
    }
}
