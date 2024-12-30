package com.tracker.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubReadService {

    // These values can be set in application.properties or environment variables
    @Value("${github.repo.owner}")
    private String owner;

    @Value("${github.repo.name}")
    private String repo;

    @Value("${github.repo.branch:main}") // default to "main" if not specified
    private String branch;

    @Value("${github.token}")
    private String githubToken;

    public String fetchMarkdownForLocalDate(LocalDate localDate) {
        // e.g. "2024-12"
        String yearMonthFolder = localDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        WeekFields customWeekFields = WeekFields.of(DayOfWeek.SUNDAY, 1);

        // e.g. 52 for the 52nd ISO week of the year
        int isoWeekNumber = localDate.get(customWeekFields.weekOfWeekBasedYear());
        String weekFolder = String.format("W%02d", isoWeekNumber);;  // e.g. "W52"

        // e.g. "2024-12-26.md"
        String dailyFileName = localDate.toString() + ".md";

        // Fetch the file for localDate's path
        return fetchMarkdownFile(yearMonthFolder, weekFolder, dailyFileName);
    }

    public Map<LocalDate, String> fetchNotesInRange(LocalDate startDate, LocalDate endDate) {
        // We will store day -> noteContent
        Map<LocalDate, String> notesMap = new HashMap<>();

        // Loop from startDate to endDate (inclusive)
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            // fetch the file for this day's path
            String noteContent = fetchMarkdownForLocalDate(current);

            // store result in the map (could be null/empty if file not found)
            notesMap.put(current, noteContent);

            // move to the next day
            current = current.plusDays(1);
        }

        // Return the map of date -> note content
        return notesMap;
    }


    public String fetchMarkdownFile(String yearMonthFolder, String weekFolder, String dailyFileName) {
        // Construct the sub-path, e.g. "2024-12/W52/2024-12-26.md"
        String fullPath = String.format("%s/%s/%s", yearMonthFolder, weekFolder, dailyFileName);

        // Build the GitHub API URL
        String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                owner, repo, fullPath, branch
        );
        // Example final URL:
        // https://api.github.com/repos/OWNER/REPO/contents/2024-12/W52/2024-12-26.md?ref=main

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        headers.set("Accept", "application/vnd.github.raw");
        // This tells GitHub we want the raw file content

        // Build the request
        HttpEntity<?> request = new HttpEntity<>(headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.GET,
                request,
                String.class
        );

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody(); // Raw markdown content
        } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            // The file might not exist yet for today
            // Handle this gracefully (return empty or throw exception)
            return null;
        } else {
            throw new RuntimeException("Error fetching file from GitHub: " + response.getStatusCode());
        }
    }
}
