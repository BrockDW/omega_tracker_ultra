package com.tracker.demo.service;

import com.tracker.demo.sql.entity.KeyBrPracticeRecord;
import com.tracker.demo.sql.repository.KeyBrPracticeRecordRepository;
import com.tracker.demo.util.CookieManager;
import com.tracker.demo.util.ScreenshotUtil;
import com.tracker.demo.util.WebDriverManager;
import com.tracker.demo.util.WebElementUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
public class KeybrScraperServiceV2 {

    // Google credentials from application.yml
    @Value("${keybr.auth.google.email}")
    private String googleEmail;

    @Value("${keybr.auth.google.password}")
    private String googlePassword;

    // Screenshot path with default fallback
    @Value("${screenshot.path:./screenshots}")
    private String screenshotPath;

    // Inject our WebDriverManager
    @Autowired
    private WebDriverManager webDriverManager;

    @Autowired
    private KeyBrPracticeRecordRepository keyBrPracticeRecordRepository;

    public String getPracticeTimeWithSession() {
        WebDriver driver = null;
        try {
            // 1. Setup driver (headless = true, for example)
            driver = webDriverManager.createChromeDriver(true);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            // 2. Attempt cookie login
            if (!attemptLoginWithCookies(driver, wait)) {
                // 3. If cookie login fails, do Google login
                doFullLoginFlow(driver, wait);
            }

            // 4. Scrape the practice time
            return getPracticeTime(driver, wait);

        } catch (Exception e) {
            System.err.println("Failed to get practice time: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        } finally {
            // Cleanup
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    System.err.println("Error during cleanup: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Attempts to load cookies from file and navigate to Keybr.
     * If the user is already logged in from stored cookies,
     * the "Practice" button should be clickable.
     */
    private boolean attemptLoginWithCookies(WebDriver driver, WebDriverWait wait) {
        try {
            driver.get("https://www.keybr.com/");
            boolean cookiesLoaded = CookieManager.loadCookies(driver);
            if (!cookiesLoaded) {
                return false;
            }

            // Refresh to apply cookies
            driver.navigate().refresh();
            Thread.sleep(2000);

            // If the user is logged in, "Practice" button should be clickable
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='Practice']")));
            System.out.println("Successfully logged in via saved cookies.");
            return true;
        } catch (Exception e) {
            System.err.println("Cookie login failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Performs the full Google login flow and then saves cookies.
     */
    private void doFullLoginFlow(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        ScreenshotUtil screenshotUtil = new ScreenshotUtil(screenshotPath);
        GoogleLoginService googleLogin = new GoogleLoginService(googleEmail, googlePassword, screenshotUtil);

        // Navigate to keybr.com
        driver.get("https://www.keybr.com/");
        WebElementUtils.waitForPageLoad(driver, Duration.ofSeconds(30));

        // Click sign-in
        By signInLocator = By.xpath("//span[contains(@class, 'SKK4yTkTJW') and text()='Sign-In']");
        WebElement signInButton =
                WebElementUtils.waitForElementClickable(driver, signInLocator, Duration.ofSeconds(30));
        WebElementUtils.clickElementWithJS(driver, signInButton);
        Thread.sleep(2000);

        // Click "Sign-in with Google"
        By googleSignInLocator = By.xpath("//button[contains(., 'Sign-in with Google')]");
        WebElement googleSignInButton =
                WebElementUtils.waitForElementClickable(driver, googleSignInLocator, Duration.ofSeconds(30));
        WebElementUtils.clickElementWithJS(driver, googleSignInButton);
        Thread.sleep(3000);

        // If a new window opened, handle it in googleLogin
        String mainWindow = driver.getWindowHandle();
        googleLogin.loginWithGoogle(driver, mainWindow);

        // Wait for Keybr main page
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//span[text()='Practice']")));

        // ===== NEW: Save cookies to file =====
        CookieManager.saveCookies(driver);
        System.out.println("Cookies saved after successful Google login.");
    }

    /**
     * Scrapes the daily goal text, parses the result, and stores the record.
     */
    private String getPracticeTime(WebDriver driver, WebDriverWait wait) throws InterruptedException {
        // Click "Practice"
        By practiceButtonLocator = By.xpath("//span[text()='Practice']");
        WebElement practiceButton = wait.until(ExpectedConditions.elementToBeClickable(practiceButtonLocator));
        WebElementUtils.clickElementWithJS(driver, practiceButton);

        Thread.sleep(2000);

        // Get daily goal text
        By dailyGoalLocator = By.xpath(
                "//span[contains(text(), 'Daily goal:')]/following-sibling::span" +
                        "//span[contains(@class, 'ZiMQFjE365')]"
        );
        WebElement dailyGoalText = wait.until(ExpectedConditions.presenceOfElementLocated(dailyGoalLocator));
        String dailyGoalValue = dailyGoalText.getText();

        // dailyGoalValue example: "40%/5 minutes"
        String[] parts = dailyGoalValue.split("/");
        if (parts.length == 2) {
            String percentageStr = parts[0].replace("%", "").trim();
            String totalMinutesStr = parts[1].replace("minutes", "").trim();

            double percentage = Double.parseDouble(percentageStr);
            double totalMinutes = Double.parseDouble(totalMinutesStr);
            double minutesPracticed = (percentage / 100.0) * totalMinutes;

            keyBrPracticeRecordRepository.upsertPracticeRecord(LocalDate.now(), minutesPracticed);

            return String.format(
                    "Total goal: %.0f minutes, Completed: %.1f minutes (%.1f%%)",
                    totalMinutes, minutesPracticed, percentage
            );
        }
        return "Could not parse practice time";
    }
}
