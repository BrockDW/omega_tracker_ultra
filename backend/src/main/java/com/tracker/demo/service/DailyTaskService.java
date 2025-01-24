package com.tracker.demo.service;

import com.tracker.demo.dto.Task;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DailyTaskService {

    private final GitHubReadService gitHubReadService;
    private static final Pattern CHECKBOX_PATTERN = Pattern.compile("^- \\[([ xX])\\] (.*)$");

    public DailyTaskService(GitHubReadService GitHubReadService) {
        this.gitHubReadService = GitHubReadService;
    }


    public List<Task> fetchMarkdownLocalDate(LocalDate localDate) {
        // e.g. "2024-12"
        String yearMonthFolder = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        WeekFields customWeekFields = WeekFields.of(DayOfWeek.SUNDAY, 1);

        // e.g. 52 for the 52nd ISO week of the year
        int isoWeekNumber = localDate.get(customWeekFields.weekOfWeekBasedYear());
        String weekFolder = String.format("W%02d", isoWeekNumber);;  // e.g. "W52"

        // e.g. "2024-12-26.md"
        String dailyFileName = localDate.toString() + ".md";

        // Fetch the file for localDate's path
        String mdContent = gitHubReadService.getContentAPI(String.format("%s/%s/%s", yearMonthFolder, weekFolder, dailyFileName));

        if (mdContent == null || mdContent.isEmpty()) {
            return Collections.emptyList();
        }

        return parseMarkdown(mdContent);
    }

    public Map<LocalDate, List<Task>> fetchMarkdownInRange(LocalDate startDate, LocalDate endDate) {
        // We will store day -> noteContent
        Map<LocalDate, List<Task>> notesMap = new HashMap<>();

        // Loop from startDate to endDate (inclusive)
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // fetch the file for this day's path
            List<Task> noteContent = fetchMarkdownLocalDate(current);

            // store result in the map (could be null/empty if file not found)
            notesMap.put(current, noteContent);

            // move to the next day
            current = current.plusDays(1);
        }

        // Return the map of date -> note content
        return notesMap;
    }

//    public List<Task> getTasksLocalDate(LocalDate localDate) {
//        String mdContent = gitHubReadService.fetchMarkdownLocalDate(localDate);
//        if (mdContent == null || mdContent.isEmpty()) {
//            return Collections.emptyList();
//        }
//        return parseMarkdown(mdContent);
//    }

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
