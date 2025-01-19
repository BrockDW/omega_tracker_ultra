package com.tracker.demo.service;

import com.tracker.demo.util.CookieManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Arrays;

@Service
public class KeybrScraperService {
    @Value("${keybr.auth.google.email}")
    private String googleEmail;

    @Value("${keybr.auth.google.password}")
    private String googlePassword;

    public String getPracticeTimeWithSession() {
        WebDriver driver = null;
        try {
            // Setup new driver for this session
            driver = setupDriver();
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // Login
            loginWithGoogle(driver, wait);

            // Get practice time
            return getPracticeTime(driver, wait);

        } catch (Exception e) {
            System.err.println("Failed to get practice time: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    System.err.println("Error during cleanup: " + e.getMessage());
                }
            }
        }
    }

    private WebDriver setupDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--no-sandbox");
        options.addArguments("--start-maximized");
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));

        WebDriver driver = new ChromeDriver(options);
        ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});"
        );

        System.out.println("Driver initialized successfully");
        return driver;
    }

    private boolean attemptLoginWithCookies(WebDriver driver, WebDriverWait wait) {
        try {
            driver.get("https://www.keybr.com/");
            boolean cookiesLoaded = CookieManager.loadCookies(driver);
            if (!cookiesLoaded) {
                return false;
            }

            driver.navigate().refresh();
            Thread.sleep(2000);

            try {
                wait.until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//span[text()='Practice']")
                ));
                return true;
            } catch (Exception e) {
                return false;
            }
        } catch (Exception e) {
            System.err.println("Cookie login failed: " + e.getMessage());
            return false;
        }
    }

    private void loginWithGoogle(WebDriver driver, WebDriverWait wait) {
        try {
            if (attemptLoginWithCookies(driver, wait)) {
                System.out.println("Successfully logged in using saved cookies");
                return;
            }

            // Your existing login code, but using the passed driver and wait
            System.out.println("Cookie login failed, proceeding with normal login...");
            driver.get("https://www.keybr.com/");

            // ... rest of your login code, replacing 'this.driver' with 'driver'
            // and 'this.wait' with 'wait'

        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    private String getPracticeTime(WebDriver driver, WebDriverWait wait) {
        try {
            WebElement practiceButton = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//span[text()='Practice']")
            ));
            clickElementWithJS(driver, practiceButton);
            System.out.println("Clicked Practice button");

            Thread.sleep(2000);

            WebElement dailyGoalText = wait.until(ExpectedConditions.presenceOfElementLocated(
                    By.xpath("//span[contains(text(), 'Daily goal:')]/following-sibling::span//span[contains(@class, 'ZiMQFjE365')]")
            ));

            String dailyGoalValue = dailyGoalText.getText();
            System.out.println("Found daily goal: " + dailyGoalValue);

            String[] parts = dailyGoalValue.split("/");
            if (parts.length == 2) {
                String percentageStr = parts[0].replace("%", "").trim();
                String totalMinutesStr = parts[1].replace("minutes", "").trim();

                double percentage = Double.parseDouble(percentageStr);
                double totalMinutes = Double.parseDouble(totalMinutesStr);
                double minutesPracticed = (percentage / 100.0) * totalMinutes;

                return String.format("Total goal: %.0f minutes, Completed: %.1f minutes (%.1f%%)",
                        totalMinutes, minutesPracticed, percentage);
            }

            return "Could not parse practice time";
        } catch (Exception e) {
            throw new RuntimeException("Error getting practice time: " + e.getMessage(), e);
        }
    }

    private void clickElementWithJS(WebDriver driver, WebElement element) {
        try {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        } catch (Exception e) {
            System.err.println("JS click failed: " + e.getMessage());
            element.click();
        }
    }
}