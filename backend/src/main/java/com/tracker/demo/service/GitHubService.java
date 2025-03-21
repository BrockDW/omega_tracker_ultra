package com.tracker.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class GitHubService {

    @Value("${github.repo.owner}")
    private String owner;

    @Value("${github.repo.name}")
    private String repo;

    @Value("${github.repo.branch:main}") // default to "main" if not specified
    private String branch;

    @Value("${github.token}")
    private String githubToken;

    /**
     * Fetch a file from GitHub in raw format.
     * Returns the file's raw content or null if not found.
     */
    public String getContentAPI(String fullPath) {
        String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/contents/%s?ref=%s",
                owner, repo, fullPath, branch
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(githubToken);
        headers.set("Accept", "application/vnd.github.raw");

        HttpEntity<?> request = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.GET, request, String.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody(); // Raw markdown content
            } else {
                throw new RuntimeException("Error fetching file from GitHub: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                return null; // File does not exist
            }
            throw e; // Other errors rethrown
        }
    }

    /**
     * Create or update a file in GitHub with the given content.
     * If the file exists, we will retrieve its 'sha' and include it in the PUT.
     * If it doesn't exist, we won't include 'sha', so GitHub will create it.
     *
     * @param fullPath      e.g. "2024-12/W52/2024-12-26.md"
     * @param newContent    the file content (plain text)
     * @param commitMessage commit message to show in GitHub
     */
    public void createOrUpdateFile(String fullPath, String newContent, String commitMessage) {
        // 1) Build the GitHub API URL (JSON version for reading file metadata)
        String apiUrl = String.format(
                "https://api.github.com/repos/%s/%s/contents/%s",
                owner, repo, fullPath
        );

        // 2) Attempt to get the JSON metadata about the file so we can retrieve 'sha' if it exists
        String existingSha = null;
        try {
            HttpHeaders getHeaders = new HttpHeaders();
            getHeaders.setBearerAuth(githubToken);
            getHeaders.set("Accept", "application/vnd.github+json");

            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl + "?ref=" + branch,
                    HttpMethod.GET,
                    new HttpEntity<>(getHeaders),
                    Map.class
            );
            if (response.getStatusCode() == HttpStatus.OK) {
                // The file exists; we can pull 'sha' from the JSON
                Map body = response.getBody();
                if (body != null && body.containsKey("sha")) {
                    existingSha = (String) body.get("sha");
                }
            }
        } catch (HttpClientErrorException e) {
            // If 404, the file doesn't exist yet, so 'existingSha' remains null
            if (!e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
                throw e;
            }
        }

        // 3) Prepare the JSON body for the PUT request
        // We must base64-encode the newContent
        String base64Encoded = Base64
                .getEncoder()
                .encodeToString(newContent.getBytes(StandardCharsets.UTF_8));

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("message", commitMessage);
        bodyMap.put("content", base64Encoded);
        bodyMap.put("branch", branch);
        if (existingSha != null) {
            // If the file existed, we include the old sha
            bodyMap.put("sha", existingSha);
        }

        // 4) Make the PUT request
        HttpHeaders putHeaders = new HttpHeaders();
        putHeaders.setBearerAuth(githubToken);
        putHeaders.setContentType(MediaType.APPLICATION_JSON);
        putHeaders.set("Accept", "application/vnd.github+json");

        HttpEntity<Map<String, Object>> putRequest = new HttpEntity<>(bodyMap, putHeaders);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> putResponse = restTemplate.exchange(
                apiUrl,
                HttpMethod.PUT,
                putRequest,
                String.class
        );

        if (putResponse.getStatusCode() != HttpStatus.CREATED
                && putResponse.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("Failed to create/update file. Status: " + putResponse.getStatusCode()
                    + " Body: " + putResponse.getBody());
        }

        // If successful, GitHub returns JSON describing the commit
        System.out.println("Successfully created/updated file at " + fullPath
                + " with commit message: " + commitMessage);
    }
}
