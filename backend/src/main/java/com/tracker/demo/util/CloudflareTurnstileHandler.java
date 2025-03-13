package com.tracker.demo.util;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

// TODO: This class is deprecated, will modify it later

public class CloudflareTurnstileHandler {
    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    public CloudflareTurnstileHandler(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        this.js = (JavascriptExecutor) driver;
    }

    public void handleTurnstile() {
        try {
            // Wait for page load
            wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                    .executeScript("return document.readyState").equals("complete"));

            System.out.println("Current URL: " + driver.getCurrentUrl());

            // First check if we're already on ChatGPT (no Cloudflare)
            if (driver.getCurrentUrl().contains("chat.openai.com")) {
                System.out.println("Already on ChatGPT - no Cloudflare challenge needed");
                return;
            }

            // Look for Cloudflare elements
            try {
                // Look for the login button to determine if we're already past Cloudflare
                WebElement loginBtn = driver.findElement(By.cssSelector("[data-testid='login-button']"));
                if (loginBtn != null && loginBtn.isDisplayed()) {
                    System.out.println("Found login button - already past Cloudflare");
                    return;
                }
            } catch (NoSuchElementException e) {
                // Login button not found, continue with Cloudflare handling
            }

            // Print page source for debugging
            System.out.println("Page source: " + driver.getPageSource());

            // Wait for Turnstile iframe (if it exists)
            wait.until(driver -> {
                try {
                    java.util.List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
                    System.out.println("Found " + iframes.size() + " iframes");
                    for (WebElement iframe : iframes) {
                        String src = iframe.getAttribute("src");
                        System.out.println("Iframe src: " + src);
                        if (src != null && (
                                src.contains("challenges.cloudflare.com") ||
                                        src.contains("turnstile") ||
                                        src.contains("cf-chl-widget")
                        )) {
                            return true;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error checking iframes: " + e.getMessage());
                }
                return false;
            });

            // Find and click the checkbox
            java.util.List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            for (WebElement iframe : iframes) {
                try {
                    driver.switchTo().frame(iframe);
                    WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(
                            By.cssSelector("[type='checkbox'], .cf-turnstile-checkbox, [role='checkbox']")
                    ));
                    checkbox.click();
                    System.out.println("Successfully clicked checkbox");
                    driver.switchTo().defaultContent();
                    break;
                } catch (Exception e) {
                    driver.switchTo().defaultContent();
                    continue;
                }
            }

            // Wait for verification to complete
            Thread.sleep(5000);

        } catch (Exception e) {
            System.out.println("Error in handleTurnstile: " + e.getMessage());
            e.printStackTrace();
            driver.switchTo().defaultContent();
        }
    }

    public void debugPage() {
        try {
            System.out.println("Current URL: " + driver.getCurrentUrl());
            System.out.println("Page title: " + driver.getTitle());

            // Print all iframe information
            java.util.List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            System.out.println("\nFound " + iframes.size() + " iframes:");
            for (WebElement iframe : iframes) {
                System.out.println("Iframe attributes:");
                System.out.println("- src: " + iframe.getAttribute("src"));
                System.out.println("- id: " + iframe.getAttribute("id"));
                System.out.println("- class: " + iframe.getAttribute("class"));
                System.out.println("---");
            }

            // Print all forms
            java.util.List<WebElement> forms = driver.findElements(By.tagName("form"));
            System.out.println("\nFound " + forms.size() + " forms:");
            for (WebElement form : forms) {
                System.out.println("Form ID: " + form.getAttribute("id"));
            }

            // Take screenshot
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), Path.of("debug_screenshot.png"));
            System.out.println("Screenshot saved as debug_screenshot.png");

        } catch (Exception e) {
            System.out.println("Error in debug: " + e.getMessage());
        }
    }
}