package com.tracker.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

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


    public String getContentAPI(String fullPath) {
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
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody(); // Raw markdown content
            } else {
                throw new RuntimeException("Error fetching file from GitHub: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e){
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return null;
            }
            throw e;
        }
    }
}
