package com.tracker.demo.service;

import com.tracker.demo.dto.Task;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public DailyTaskService(GitHubReadService gitHubReadService) {
        this.gitHubReadService = gitHubReadService;
    }

    // ------------------------------------------------------------------------
    // 1) FETCH A SINGLE DAY’S TASKS
    // ------------------------------------------------------------------------
    public List<Task> fetchMarkdownLocalDate(LocalDate localDate) {
        // e.g. "2024-12"
        String yearMonthFolder = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        WeekFields customWeekFields = WeekFields.of(DayOfWeek.SUNDAY, 1);

        // e.g. 52 for the 52nd ISO week of the year
        int isoWeekNumber = localDate.get(customWeekFields.weekOfWeekBasedYear());
        String weekFolder = String.format("W%02d", isoWeekNumber);  // e.g. "W52"

        // e.g. "2024-12-26.md"
        String dailyFileName = localDate.toString() + ".md";

        // Fetch the file for localDate's path
        String mdContent = gitHubReadService.getContentAPI(
                String.format("%s/%s/%s", yearMonthFolder, weekFolder, dailyFileName)
        );

        if (mdContent == null || mdContent.isEmpty()) {
            return Collections.emptyList();
        }
        return parseMarkdown(mdContent);
    }

    // ------------------------------------------------------------------------
    // 2) FETCH TASKS OVER A DATE RANGE
    // ------------------------------------------------------------------------
    public Map<LocalDate, List<Task>> fetchMarkdownInRange(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, List<Task>> notesMap = new HashMap<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            List<Task> noteContent = fetchMarkdownLocalDate(current);
            notesMap.put(current, noteContent);
            current = current.plusDays(1);
        }

        return notesMap;
    }

    // ------------------------------------------------------------------------
    // 3) HELPER TO PARSE MARKDOWN INTO TASKS
    //    (we do not apply exclusion here)
    // ------------------------------------------------------------------------
    private List<Task> parseMarkdown(String markdown) {
        if (markdown == null) return Collections.emptyList();

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

    // ------------------------------------------------------------------------
    // 4) FETCH WEEKLY MARKDOWN
    // ------------------------------------------------------------------------
    private List<Task> fetchWeeklyMarkdown(LocalDate date) {
        String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        WeekFields customWeekFields = WeekFields.of(DayOfWeek.SUNDAY, 1);
        int weekNumber = date.get(customWeekFields.weekOfWeekBasedYear());
        String weeklyFileName = "Weekly.md";

        String path = String.format("%s/W%02d/%s", yearMonth, weekNumber, weeklyFileName);
        String content = gitHubReadService.getContentAPI(path);

        return parseMarkdown(content != null ? content : "");
    }

    // ------------------------------------------------------------------------
    // 5) FETCH MONTHLY MARKDOWN
    // ------------------------------------------------------------------------
    private List<Task> fetchMonthlyMarkdown(LocalDate date) {
        String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String monthlyFileName = "Monthly.md";

        String path = String.format("%s/%s", yearMonth, monthlyFileName);
        String content = gitHubReadService.getContentAPI(path);

        return parseMarkdown(content != null ? content : "");
    }

    // ------------------------------------------------------------------------
    // 6) AGGREGATED INCOMPLETE TASKS (uses exclusion list)
    // ------------------------------------------------------------------------
    public Map<String, List<Task>> fetchAggregatedIncompleteTasks(LocalDate startDate, LocalDate endDate) {
        // 1) Read the local exclusion file. We only do this once here,
        //    so if the file is large, you might want to cache it globally.
        //    For simplicity, we'll read each time this method is called.
        Set<String> exclusionSet = loadExclusionSet("task_exclusion_list.md");

        List<Task> dailyTasks = new ArrayList<>();
        List<Task> weeklyTasks = new ArrayList<>();
        List<Task> monthlyTasks = new ArrayList<>();

        Set<String> fetchedWeeks = new HashSet<>();
        Set<String> fetchedMonths = new HashSet<>();

        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // ---------------------------
            // Daily tasks
            // ---------------------------
            List<Task> daily = fetchMarkdownLocalDate(current);
            // filter incomplete
            daily = daily.stream()
                    .filter(task -> !task.isCompleted())
                    .collect(Collectors.toList());
            // mark excluded if found in set
            markExcluded(daily, exclusionSet);
            dailyTasks.addAll(daily);

            // ---------------------------
            // Weekly tasks (fetch once per week)
            // ---------------------------
            WeekFields weekFields = WeekFields.of(DayOfWeek.SUNDAY, 1);
            int weekNumber = current.get(weekFields.weekOfWeekBasedYear());
            String weekKey = current.getYear() + "-W" + weekNumber;

            if (!fetchedWeeks.contains(weekKey)) {
                List<Task> wTasks = fetchWeeklyMarkdown(current);
                // only incomplete
                wTasks = wTasks.stream()
                        .filter(task -> !task.isCompleted())
                        .collect(Collectors.toList());
                // mark excluded
                markExcluded(wTasks, exclusionSet);

                weeklyTasks.addAll(wTasks);
                fetchedWeeks.add(weekKey);
            }

            // ---------------------------
            // Monthly tasks (fetch once per month)
            // ---------------------------
            String monthKey = current.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            if (!fetchedMonths.contains(monthKey)) {
                List<Task> mTasks = fetchMonthlyMarkdown(current);
                // only incomplete
                mTasks = mTasks.stream()
                        .filter(task -> !task.isCompleted())
                        .collect(Collectors.toList());
                // mark excluded
                markExcluded(mTasks, exclusionSet);

                monthlyTasks.addAll(mTasks);
                fetchedMonths.add(monthKey);
            }

            current = current.plusDays(1);
        }

        // 2) Build result map
        Map<String, List<Task>> result = new HashMap<>();
        result.put("dailyTasks", dailyTasks);
        result.put("weeklyTasks", weeklyTasks);
        result.put("monthlyTasks", monthlyTasks);

        return result;
    }

    // ------------------------------------------------------------------------
    // HELPER: Load lines from local exclusion file into a set (case-insensitive)
    // ------------------------------------------------------------------------
    private Set<String> loadExclusionSet(String fileName) {
        try {
            // read lines from the local file
            List<String> lines = Files.readAllLines(Path.of(fileName));
            // parse them similarly with parseMarkdown
            List<Task> tasks = parseMarkdown(String.join("\n", lines));
            // we only need the text descriptions in lower case
            return tasks.stream()
                    .map(t -> t.getDescription().toLowerCase())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            // If file not found or can't be read, return empty set
            return Collections.emptySet();
        }
    }

    // ------------------------------------------------------------------------
    // HELPER: mark tasks as excluded
    // ------------------------------------------------------------------------
    private void markExcluded(List<Task> tasks, Set<String> exclusionSet) {
        for (Task task : tasks) {
            String descLower = task.getDescription().toLowerCase();
            if (exclusionSet.contains(descLower)) {
                task.setExcluded(true);
            }
        }
    }

    public void setExclusion(String description, boolean excluded) {
        // 1) We'll read the file lines (if it exists), store them in memory
        // 2) We'll see if "description" is present (case-insensitive)
        // 3) If excluded == true and it's NOT present, we add it
        // 4) If excluded == false and it's present, we remove it
        // 5) Then write the updated lines back to disk

        Path filePath = Path.of("task_exclusion_list.md");
        List<String> lines = new ArrayList<>();

        try {
            if (Files.exists(filePath)) {
                lines = Files.readAllLines(filePath);
            }
        } catch (IOException e) {
            // handle read error if needed
        }

        // We'll create a helper function that turns the line
        // into a normalized "description" for comparison
        Pattern pattern = Pattern.compile("^- \\[[ xX]\\] (.*)$", Pattern.CASE_INSENSITIVE);

        // Convert "lines" into a list of (originalLine, normalizedDesc)
        List<LinePair> linePairs = new ArrayList<>();
        for (String line : lines) {
            Matcher m = pattern.matcher(line);
            if (m.matches()) {
                String desc = m.group(1).trim(); // text after `- [ ] `
                linePairs.add(new LinePair(line, desc.toLowerCase()));
            } else {
                // Not a task line, keep as-is
                linePairs.add(new LinePair(line, null));
            }
        }

        String normalizedInputDesc = description.toLowerCase();
        boolean alreadyPresent = linePairs.stream()
                .anyMatch(lp -> normalizedInputDesc.equals(lp.desc));

        // If we want to "exclude" and it's not already in the file -> add it
        if (excluded && !alreadyPresent) {
            // We'll store it as "- [ ] " + original (unmodified) description
            String newLine = "- [ ] " + description;
            linePairs.add(new LinePair(newLine, normalizedInputDesc));
        }

        // If we want to "un-exclude" (excluded == false) and it is present -> remove it
        if (!excluded && alreadyPresent) {
            // remove any line that has matching desc
            linePairs.removeIf(lp -> normalizedInputDesc.equals(lp.desc));
        }

        // Rebuild final lines
        List<String> newLines = linePairs.stream()
                .map(lp -> lp.originalLine)
                .collect(Collectors.toList());

        // Overwrite the file
        try {
            Files.write(filePath, newLines);
        } catch (IOException e) {
            // handle write error
        }
    }

    // Helper class for in-memory representation of lines
    private static class LinePair {
        String originalLine;  // e.g. "- [ ] Something"
        String desc;          // normalized description or null if not a valid pattern

        LinePair(String originalLine, String desc) {
            this.originalLine = originalLine;
            this.desc = desc;
        }
    }
}
