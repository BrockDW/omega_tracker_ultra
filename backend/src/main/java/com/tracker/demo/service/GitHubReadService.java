package com.tracker.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;

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

    public String fetchTodayMarkdown() {
        LocalDate today = LocalDate.now();

        // e.g. "2024-12"
        String yearMonthFolder = today.format(DateTimeFormatter.ofPattern("yyyy-MM"));

        // e.g. 52 for the 52nd ISO week of the year
        int isoWeekNumber = today.get(WeekFields.ISO.weekOfWeekBasedYear());
        String weekFolder = "W" + isoWeekNumber;  // e.g. "W52"

        // e.g. "2024-12-26.md"
        String dailyFileName = today.toString() + ".md";

        // Fetch the file for today's path
        return fetchMarkdownFile(yearMonthFolder, weekFolder, dailyFileName);
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
