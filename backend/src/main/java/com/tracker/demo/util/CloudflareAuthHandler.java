package com.tracker.demo.util;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;

// TODO: This class is deprecated, will modify it later

public class CloudflareAuthHandler {
    private WebDriver driver;
    private WebDriverWait wait;
    private JavascriptExecutor js;

    public CloudflareAuthHandler(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    public void handleCloudflareAuth() {
        try {
            // Wait for Cloudflare challenge to load and complete
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    By.cssSelector("div#cf-spinner")
            ));

            // Additional wait to ensure the page is fully loaded
            Thread.sleep(5000);

            // Verify we're past Cloudflare
            wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("body:not(.challenge-running)")
            ));

        } catch (Exception e) {
            throw new RuntimeException("Failed to handle Cloudflare authentication", e);
        }
    }

    public void handleCaptchaCheckbox() {
        try {
            // Switch to the iframe containing the checkbox
            WebElement iframe = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.cssSelector("iframe[src*='challenges']")
            ));
            driver.switchTo().frame(iframe);

            // Wait for and click the checkbox
            WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("input[type='checkbox']")
            ));
            checkbox.click();

            // Switch back to default content
            driver.switchTo().defaultContent();

            // Wait for verification to complete
            Thread.sleep(5000);

        } catch (Exception e) {
            throw new RuntimeException("Failed to handle Cloudflare CAPTCHA checkbox", e);
        }
    }

    public void diagnoseCloudflareElements() {
        try {
            // Take screenshot
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), Path.of("cloudflare_page.png"));
            System.out.println("Screenshot saved as cloudflare_page.png");

            // Get page source
            String pageSource = driver.getPageSource();
            Files.writeString(Path.of("page_source.html"), pageSource, StandardOpenOption.CREATE);
            System.out.println("Page source saved as page_source.html");

            // List all iframes
            List<WebElement> iframes = driver.findElements(By.tagName("iframe"));
            System.out.println("\nFound " + iframes.size() + " iframes:");
            for (WebElement iframe : iframes) {
                System.out.println("iframe attributes:");
                System.out.println("- src: " + iframe.getAttribute("src"));
                System.out.println("- id: " + iframe.getAttribute("id"));
                System.out.println("- class: " + iframe.getAttribute("class"));
                System.out.println("- title: " + iframe.getAttribute("title"));
                System.out.println("---");
            }

            // List all elements with 'checkbox' in their attributes
            List<WebElement> checkboxElements = driver.findElements(
                    By.cssSelector("[type='checkbox'], [role='checkbox'], [class*='checkbox']")
            );
            System.out.println("\nFound " + checkboxElements.size() + " potential checkbox elements:");
            for (WebElement checkbox : checkboxElements) {
                System.out.println("checkbox attributes:");
                System.out.println("- id: " + checkbox.getAttribute("id"));
                System.out.println("- class: " + checkbox.getAttribute("class"));
                System.out.println("- type: " + checkbox.getAttribute("type"));
                System.out.println("- role: " + checkbox.getAttribute("role"));
                System.out.println("---");
            }

        } catch (Exception e) {
            System.out.println("Error during diagnosis: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isCaptchaPresent() {
        try {
            // Try multiple known Cloudflare selectors
            List<String> cloudflareSelectors = List.of(
                    "iframe[src*='challenges']",
                    "iframe[src*='cloudflare']",
                    "iframe[title*='challenge']",
                    "iframe#cf-challenge-iframe",
                    "#challenge-form",
                    "#challenge-stage"
            );

            // Check each selector
            for (String selector : cloudflareSelectors) {
                try {
                    List<WebElement> elements = driver.findElements(By.cssSelector(selector));
                    if (!elements.isEmpty() && elements.get(0).isDisplayed()) {
                        System.out.println("Found Cloudflare element with selector: " + selector);
                        return true;
                    }
                } catch (Exception e) {
                    // Continue checking other selectors
                    continue;
                }
            }

            // Also check for specific text that might indicate Cloudflare
            String pageSource = driver.getPageSource().toLowerCase();
            if (pageSource.contains("cloudflare") ||
                    pageSource.contains("security check") ||
                    pageSource.contains("verify you are human")) {
                System.out.println("Found Cloudflare reference in page source");
                return true;
            }

            return false;

        } catch (Exception e) {
            System.out.println("Error checking for CAPTCHA: " + e.getMessage());
            return false;
        }
    }

    public void handleTurnstile() {
        try {
            // Wait for Turnstile iframe to be present (it's loaded dynamically)
            wait.until(driver -> {
                try {
                    return !driver.findElements(By.xpath("//iframe[contains(@src, 'challenges.cloudflare.com')]")).isEmpty();
                } catch (Exception e) {
                    return false;
                }
            });

            // Find all iframes and look for the Turnstile one
            for (WebElement iframe : driver.findElements(By.tagName("iframe"))) {
                String src = iframe.getAttribute("src");
                if (src != null && src.contains("challenges.cloudflare.com")) {
                    System.out.println("Found Turnstile iframe: " + src);

                    // Switch to the iframe
                    driver.switchTo().frame(iframe);

                    // Wait for and click the checkbox (using multiple possible selectors)
                    wait.until(ExpectedConditions.elementToBeClickable(
                            By.cssSelector("[type='checkbox'], .cf-turnstile-checkbox, [role='checkbox']")
                    )).click();

                    // Switch back to main content
                    driver.switchTo().defaultContent();

                    // Wait for verification
                    Thread.sleep(5000);
                    break;
                }
            }

        } catch (Exception e) {
            driver.switchTo().defaultContent();
            throw new RuntimeException("Failed to handle Cloudflare Turnstile", e);
        }
    }

    public boolean isTurnstilePresent() {
        try {
            // Check for Turnstile-specific elements or scripts
            return (Boolean) js.executeScript(
                    "return !!document.querySelector('script[src*=\"challenges.cloudflare.com/turnstile\"]') || " +
                            "!!document.querySelector('iframe[src*=\"challenges.cloudflare.com\"]')"
            );
        } catch (Exception e) {
            return false;
        }
    }
}