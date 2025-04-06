package com.tracker.demo.service;

import com.tracker.demo.dto.Task;
import org.springframework.scheduling.annotation.Scheduled;
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

    private final GitHubService gitHubService;
    private static final Pattern CHECKBOX_PATTERN = Pattern.compile("^- \\[([ xX])\\] (.*)$");

    public DailyTaskService(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
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
        String mdContent = gitHubService.getContentAPI(
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
        String content = gitHubService.getContentAPI(path);

        return parseMarkdown(content != null ? content : "");
    }

    // ------------------------------------------------------------------------
    // 5) FETCH MONTHLY MARKDOWN
    // ------------------------------------------------------------------------
    private List<Task> fetchMonthlyMarkdown(LocalDate date) {
        String yearMonth = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String monthlyFileName = "Monthly.md";

        String path = String.format("%s/%s", yearMonth, monthlyFileName);
        String content = gitHubService.getContentAPI(path);

        return parseMarkdown(content != null ? content : "");
    }

    public Map<String, List<Task>> fetchAggregatedIncompleteTasks(LocalDate startDate, LocalDate endDate) {
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
            daily = daily.stream()
                    .filter(task -> !task.isCompleted())
                    .collect(Collectors.toList());
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
                wTasks = wTasks.stream()
                        .filter(task -> !task.isCompleted())
                        .collect(Collectors.toList());
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
                mTasks = mTasks.stream()
                        .filter(task -> !task.isCompleted())
                        .collect(Collectors.toList());
                markExcluded(mTasks, exclusionSet);

                monthlyTasks.addAll(mTasks);
                fetchedMonths.add(monthKey);
            }

            current = current.plusDays(1);
        }

        // ------------------------------------------------------------------------
        // NEW: remove duplicates + sort by frequency for each list
        // ------------------------------------------------------------------------
        List<Task> finalDaily = removeDuplicatesAndSortByFrequency(dailyTasks);
        List<Task> finalWeekly = removeDuplicatesAndSortByFrequency(weeklyTasks);
        List<Task> finalMonthly = removeDuplicatesAndSortByFrequency(monthlyTasks);

        // Build result map
        Map<String, List<Task>> result = new HashMap<>();
        result.put("dailyTasks", finalDaily);
        result.put("weeklyTasks", finalWeekly);
        result.put("monthlyTasks", finalMonthly);

        return result;
    }

    /**
     * For a list of tasks (all incomplete):
     *  1) group by description (case-sensitive or not; adjust if needed).
     *  2) each group -> create a single Task that merges them
     *  3) set task.frequency to the number of duplicates
     *  4) sort descending by that frequency
     */
    private List<Task> removeDuplicatesAndSortByFrequency(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return tasks;
        }

        Map<String, List<Task>> grouped = tasks.stream()
                // Exclude incomplete Auto Generated tasks
                .filter(t -> !(t.getDescription().endsWith("[Auto Generated]") && !t.isCompleted()))
                .collect(Collectors.groupingBy(t -> t.getDescription().toLowerCase()));

        List<Task> deduplicated = new ArrayList<>();
        for (Map.Entry<String, List<Task>> entry : grouped.entrySet()) {
            Task rep = entry.getValue().get(0);
            rep.setFrequency(entry.getValue().size());
            boolean anyExcluded = entry.getValue().stream().anyMatch(Task::isExcluded);
            rep.setExcluded(anyExcluded);
            deduplicated.add(rep);
        }

        deduplicated.sort((a, b) -> Integer.compare(b.getFrequency(), a.getFrequency()));
        return deduplicated;
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

    /**
     * Scheduled job that runs every day at 00:05 AM PST.
     *  1) Check if today's daily .md file on GitHub exists.
     *  2) If NOT, then create it with the top 2 tasks from incomplete daily,
     *     then weekly, then monthly (by descending priority).
     */
    @Scheduled(cron = "0 5 0 * * ?", zone = "America/Los_Angeles")
    public void autoCreateDailyTaskForToday() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        WeekFields customWeekFields = WeekFields.of(DayOfWeek.SUNDAY, 1);

        // ----- First: Check yesterday's markdown file for completed auto-generated tasks -----
        String yesterdayYearMonth = yesterday.format(formatter);
        int yesterdayIsoWeek = yesterday.get(customWeekFields.weekOfWeekBasedYear());
        String yesterdayWeekFolder = String.format("W%02d", yesterdayIsoWeek);
        String yesterdayFileName = yesterday.toString() + ".md";
        String yesterdayPath = String.format("%s/%s/%s", yesterdayYearMonth, yesterdayWeekFolder, yesterdayFileName);
        String yesterdayContent = gitHubService.getContentAPI(yesterdayPath);

        if (yesterdayContent != null && !yesterdayContent.isEmpty()) {
            List<Task> yesterdayTasks = parseMarkdown(yesterdayContent);
            yesterdayTasks.stream()
                    .filter(t -> t.getDescription().endsWith("[Auto Generated]") && t.isCompleted())
                    .forEach(t -> {
                        String desc = t.getDescription().replace("[Auto Generated]", "").trim();
                        setExclusion(desc, true);
                    });
        }

        // ----- Second: Check today's file existence -----
        String todayYearMonth = today.format(formatter);
        int todayIsoWeek = today.get(customWeekFields.weekOfWeekBasedYear());
        String todayWeekFolder = String.format("W%02d", todayIsoWeek);
        String todayFileName = today.toString() + ".md";
        String todayPath = String.format("%s/%s/%s", todayYearMonth, todayWeekFolder, todayFileName);
        String existingContent = gitHubService.getContentAPI(todayPath);

        if (existingContent != null && !existingContent.isEmpty()) {
            System.out.println("Today's markdown file already exists. Skipping creation.");
            return;
        }

        // ----- Third: Gather incomplete tasks excluding Auto Generated incomplete tasks -----
        LocalDate startOfYear = LocalDate.of(today.getYear(), 1, 1);
        Map<String, List<Task>> aggMap = fetchAggregatedIncompleteTasks(startOfYear, today.minusDays(1));

        List<Task> dailyList = aggMap.getOrDefault("dailyTasks", Collections.emptyList())
                .stream().filter(t -> !t.isExcluded()).collect(Collectors.toList());

        List<Task> weeklyList = aggMap.getOrDefault("weeklyTasks", Collections.emptyList())
                .stream().filter(t -> !t.isExcluded()).collect(Collectors.toList());

        List<Task> monthlyList = aggMap.getOrDefault("monthlyTasks", Collections.emptyList())
                .stream().filter(t -> !t.isExcluded()).collect(Collectors.toList());

        // ----- Pick only 1 task -----
        Optional<Task> chosenTask = pickOneFromThreeLists(dailyList, weeklyList, monthlyList);

        StringBuilder sb = new StringBuilder();
        if (chosenTask.isPresent()) {
            Task t = chosenTask.get();
            sb.append("- [ ] ").append(t.getDescription()).append(" [Auto Generated]").append("\n");
        } else {
            System.out.println("No suitable task found; creating empty markdown file.");
        }

        String newFileContent = sb.toString();

        gitHubService.createOrUpdateFile(
                todayPath,
                newFileContent,
                "Auto-created daily task file at 00:05 PST"
        );

        System.out.println("Created daily markdown file for " + today);
    }


    /**
     * Utility to pick up to 2 tasks from daily first, if not enough then from weekly,
     * if still not enough, from monthly.
     */
    private List<Task> pickTopTwoFromThreeLists(List<Task> daily, List<Task> weekly, List<Task> monthly) {
        List<Task> result = new ArrayList<>();

        // daily is already sorted by frequency desc (from removeDuplicatesAndSortByFrequency)
        // so just pick from the front
        for (Task t : daily) {
            if (result.size() >= 2) break;
            result.add(t);
        }

        // if not enough, pick from weekly
        if (result.size() < 2) {
            for (Task t : weekly) {
                if (result.size() >= 2) break;
                result.add(t);
            }
        }

        // if still not enough, pick from monthly
        if (result.size() < 2) {
            for (Task t : monthly) {
                if (result.size() >= 2) break;
                result.add(t);
            }
        }

        return result;
    }

    private Optional<Task> pickOneFromThreeLists(List<Task> daily, List<Task> weekly, List<Task> monthly) {
        if (!daily.isEmpty()) return Optional.of(daily.get(0));
        if (!weekly.isEmpty()) return Optional.of(weekly.get(0));
        if (!monthly.isEmpty()) return Optional.of(monthly.get(0));
        return Optional.empty();
    }
}
