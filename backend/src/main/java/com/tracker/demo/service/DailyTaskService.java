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
import java.util.stream.Collectors;

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

    public Map<String, List<Task>> fetchAggregatedIncompleteTasks(LocalDate startDate, LocalDate endDate) {
        List<Task> dailyTasks = new ArrayList<>();
        List<Task> weeklyTasks = new ArrayList<>();
        List<Task> monthlyTasks = new ArrayList<>();

        Set<String> fetchedWeeks = new HashSet<>();
        Set<String> fetchedMonths = new HashSet<>();

        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // Daily tasks
            dailyTasks.addAll(
                    fetchMarkdownLocalDate(current).stream()
                            .filter(task -> !task.isCompleted())
                            .collect(Collectors.toList())
            );

            // Weekly tasks (fetch once per week)
            WeekFields weekFields = WeekFields.of(DayOfWeek.SUNDAY, 1);
            int weekNumber = current.get(weekFields.weekOfWeekBasedYear());
            String weekKey = current.getYear() + "-W" + weekNumber;

            if (!fetchedWeeks.contains(weekKey)) {
                weeklyTasks.addAll(
                        fetchWeeklyMarkdown(current).stream()
                                .filter(task -> !task.isCompleted())
                                .collect(Collectors.toList())
                );
                fetchedWeeks.add(weekKey);
            }

            // Monthly tasks (fetch once per month)
            String monthKey = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));

            if (!fetchedMonths.contains(monthKey)) {
                monthlyTasks.addAll(
                        fetchMonthlyMarkdown(current).stream()
                                .filter(task -> !task.isCompleted())
                                .collect(Collectors.toList())
                );
                fetchedMonths.add(monthKey);
            }

            // move to next day
            current = current.plusDays(1);
        }

        Map<String, List<Task>> result = new HashMap<>();
        result.put("dailyTasks", dailyTasks);
        result.put("weeklyTasks", weeklyTasks);
        result.put("monthlyTasks", monthlyTasks);

        return result;
    }

    private List<Task> fetchWeeklyMarkdown(LocalDate date) {
        String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        WeekFields customWeekFields = WeekFields.of(DayOfWeek.SUNDAY, 1);
        int weekNumber = date.get(customWeekFields.weekOfWeekBasedYear());
        String weeklyFileName = "Weekly.md";

        String path = String.format("%s/W%02d/%s", yearMonth, weekNumber, weeklyFileName);
        String content = gitHubReadService.getContentAPI(path);

        return parseMarkdown(content != null ? content : "");
    }

    private List<Task> fetchMonthlyMarkdown(LocalDate date) {
        String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String monthlyFileName = "Monthly.md";

        String path = String.format("%s/%s", yearMonth, monthlyFileName);
        String content = gitHubReadService.getContentAPI(path);

        return parseMarkdown(content != null ? content : "");
    }
}
