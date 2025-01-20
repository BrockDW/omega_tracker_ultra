package com.tracker.demo.service;

import com.tracker.demo.util.CookieManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeybrScraperService {
    @Value("${keybr.auth.google.email}")
    private String googleEmail;

    @Value("${keybr.auth.google.password}")
    private String googlePassword;

    @Value("${chrome.binary:}")
    private String chromeBinary;

    @Value("${chrome.driver:}")
    private String chromeDriver;

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
        // Set system properties if paths are provided
        if (!StringUtils.isEmpty(chromeDriver)) {
            System.setProperty("webdriver.chrome.driver", chromeDriver);
        }
        if (!StringUtils.isEmpty(chromeBinary)) {
            System.setProperty("webdriver.chrome.binary", chromeBinary);
        }

        ChromeOptions options = new ChromeOptions();

        // Set binary if provided (for Linux/RPi)
        if (!StringUtils.isEmpty(chromeBinary)) {
            options.setBinary(chromeBinary);

            // Add ARM-specific arguments when binary is set (Linux/RPi case)
            options.addArguments("--remote-debugging-port=9222");
            options.addArguments("--disable-setuid-sandbox");
            options.addArguments("--disable-gpu-sandbox");
        }

        // Add stealth settings
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--no-sandbox");
        options.addArguments("--headless=new");
        options.addArguments("--disable-dev-shm-usage");

        // Add additional stealth settings
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-gpu");
        options.addArguments("--ignore-certificate-errors");
        options.addArguments("--allow-running-insecure-content");
        options.addArguments("--disable-web-security");
        options.addArguments("--lang=en-US,en;q=0.9");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // Add experimental flags
        options.setExperimentalOption("excludeSwitches", Arrays.asList(
                "enable-automation",
                "disable-popup-blocking"
        ));

        // Add additional preferences
        Map<String, Object> prefs = new HashMap<>();
        prefs.put("credentials_enable_service", false);
        prefs.put("profile.password_manager_enabled", false);
        options.setExperimentalOption("prefs", prefs);

        try {
            WebDriver driver = new ChromeDriver(options);

            // Add stealth scripts
            ((JavascriptExecutor) driver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});"
            );

            ((JavascriptExecutor) driver).executeScript(
                    "window.navigator.chrome = { runtime: {} };"
            );

            System.out.println("Driver initialized successfully");
            return driver;
        } catch (Exception e) {
            System.err.println("Failed to initialize driver: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
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

    private void waitForPageLoad(WebDriver driver, WebDriverWait wait) {
        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").equals("complete"));
    }

    private void waitForElementToBeReady(WebElement element, WebDriverWait wait) {
        wait.until(driver -> {
            try {
                return element.isDisplayed() && element.isEnabled();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });
    }

    private WebElement waitForElementSafely(WebDriver driver, By locator, WebDriverWait wait) {
        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        return element;
    }

    private void loginWithGoogle(WebDriver driver, WebDriverWait wait) {
        try {
            new File("screenshots").mkdirs();

            if (attemptLoginWithCookies(driver, wait)) {
                System.out.println("Successfully logged in using saved cookies");
                return;
            }

            System.out.println("Cookie login failed, proceeding with normal login...");
            driver.get("https://www.keybr.com/");
            System.out.println("Navigated to keybr.com");

            // Wait for page to be fully loaded
            waitForPageLoad(driver, wait);
            System.out.println("Page fully loaded");
            takeScreenshot(driver, "1_homepage");

            // Wait for all animations to complete (additional safety)
            Thread.sleep(3000);

            // Click sign-in button with robust checks
            try {
                By signInLocator = By.xpath("//span[contains(@class, 'SKK4yTkTJW') and text()='Sign-In']");
                WebElement signInButton = waitForElementSafely(driver, signInLocator, wait);

                // Double-check if button is truly clickable
                waitForElementToBeReady(signInButton, wait);
                clickElementWithJS(driver, signInButton);
                System.out.println("Clicked Sign-In button");
                takeScreenshot(driver, "2_after_signin_click");
            } catch (Exception e) {
                System.out.println("Direct click failed, navigating to account page...");
                driver.get("https://www.keybr.com/account");
                waitForPageLoad(driver, wait);
                takeScreenshot(driver, "2_account_page");
            }

            // Wait for page transition
            Thread.sleep(2000);
            waitForPageLoad(driver, wait);

            // Try multiple selectors for Google sign-in button with validation
            WebElement googleSignIn = null;
            Exception lastException = null;

            String[] googleSignInXPaths = {
                    "//button[contains(., 'SIGN-IN WITH GOOGLE')]",
                    "//button[contains(., 'Sign-in with Google')]",
                    "//button[.//div[contains(text(), 'Sign-in with Google')]]",
                    "//div[contains(@class, 'SignIn')]//button[1]"
            };

            for (String xpath : googleSignInXPaths) {
                try {
                    By locator = By.xpath(xpath);
                    googleSignIn = waitForElementSafely(driver, locator, wait);
                    if (googleSignIn != null && googleSignIn.isDisplayed() && googleSignIn.isEnabled()) {
                        System.out.println("Found Google sign-in button with xpath: " + xpath);
                        break;
                    }
                } catch (Exception e) {
                    lastException = e;
                    System.out.println("Failed to find button with xpath: " + xpath);
                }
            }

            if (googleSignIn == null) {
                throw new RuntimeException("Could not find Google sign-in button", lastException);
            }

            // Extra validation before clicking
            waitForElementToBeReady(googleSignIn, wait);
            clickElementWithJS(driver, googleSignIn);
            System.out.println("Clicked Google sign-in button");
            takeScreenshot(driver, "3_before_google_popup");

            Thread.sleep(2000);

            // Rest of the code remains exactly the same...
            String mainWindow = driver.getWindowHandle();
            System.out.println("Available windows before switch: " + driver.getWindowHandles().size());

            for(String windowHandle : driver.getWindowHandles()) {
                if(!mainWindow.equals(windowHandle)) {
                    driver.switchTo().window(windowHandle);
                    System.out.println("Switched to window: " + driver.getCurrentUrl());
                    break;
                }
            }
            takeScreenshot(driver, "4_google_login_page");

            // Print page source for debugging
            System.out.println("Current page source:");
            System.out.println(driver.getPageSource().substring(0, Math.min(500, driver.getPageSource().length())));

            // Enter email
            try {
                WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("input[type='email']")
                ));
                emailInput.sendKeys(googleEmail);
                emailInput.sendKeys(Keys.ENTER);
                System.out.println("Entered email");
                takeScreenshot(driver, "5_after_email");
            } catch (Exception e) {
                System.out.println("Failed to find email input. Available elements:");
                List<WebElement> inputs = driver.findElements(By.tagName("input"));
                inputs.forEach(input -> System.out.println("Input type: " + input.getAttribute("type")));
                takeScreenshot(driver, "5_email_error");
                throw e;
            }

            Thread.sleep(2000);

            // Enter password with additional debugging
            try {
                WebElement passwordInput = wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("input[type='password']")
                ));
                passwordInput.sendKeys(googlePassword);
                passwordInput.sendKeys(Keys.ENTER);
                System.out.println("Entered password");
                takeScreenshot(driver, "6_after_password");
            } catch (Exception e) {
                System.out.println("Failed to find password input. Current URL: " + driver.getCurrentUrl());
                System.out.println("Page source:");
                System.out.println(driver.getPageSource().substring(0, Math.min(500, driver.getPageSource().length())));
                takeScreenshot(driver, "6_password_error");
                throw e;
            }
        } catch (Exception e) {
            takeScreenshot(driver, "error_state");
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

    private void takeScreenshot(WebDriver driver, String filename) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(screenshot.toPath(), new File("screenshots/" + filename + ".png").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Screenshot saved: " + filename + ".png");
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }
}