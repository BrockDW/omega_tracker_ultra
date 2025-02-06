//package com.tracker.demo.service;
//
//import com.tracker.demo.constants.Constants;
//import com.tracker.demo.util.CookieManager;
//import org.openqa.selenium.*;
//import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.chrome.ChromeOptions;
//import org.openqa.selenium.support.ui.ExpectedConditions;
//import org.openqa.selenium.support.ui.WebDriverWait;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.nio.file.Files;
//import java.nio.file.StandardCopyOption;
//import java.time.Duration;
//import java.util.*;
//
//@Service
//public class KeybrScraperService {
//    @Value("${keybr.auth.google.email}")
//    private String googleEmail;
//
//    @Value("${keybr.auth.google.password}")
//    private String googlePassword;
//
//    @Value("${chrome.binary:}")
//    private String chromeBinary;
//
//    @Value("${chrome.driver:}")
//    private String chromeDriver;
//
//    @Scheduled(cron = "0 30 23 * * ?")
//    public void keybrScheduledTracker() {
//        getPracticeTimeWithSession();
//    }
//
//    public String getPracticeTimeWithSession() {
//        WebDriver driver = null;
//        try {
//            // Setup new driver for this session
//            driver = setupDriver();
//            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
//
//            // Login
//            loginWithGoogle(driver, wait);
//
//            // Get practice time
//            return getPracticeTime(driver, wait);
//
//        } catch (Exception e) {
//            System.err.println("Failed to get practice time: " + e.getMessage());
//            e.printStackTrace();
//            return "Error: " + e.getMessage();
//        } finally {
//            if (driver != null) {
//                try {
//                    driver.quit();
//                } catch (Exception e) {
//                    System.err.println("Error during cleanup: " + e.getMessage());
//                }
//            }
//        }
//    }
//
//    private WebDriver setupDriver() {
//        String os = System.getProperty("os.name").toLowerCase();
//        boolean isLinux = os.contains("linux");
//
//        ChromeOptions options = new ChromeOptions();
//
//        // Common critical options
//        options.addArguments("--remote-allow-origins=*");
//        options.addArguments("--disable-blink-features=AutomationControlled");
//        options.addArguments("--disable-extensions");
//        options.addArguments("--no-sandbox");
//        options.addArguments("--headless=new");
//        options.addArguments("--disable-dev-shm-usage");
//        options.addArguments("--window-size=1920,1080");
//        options.addArguments("--disable-popup-blocking");
//        options.addArguments("--disable-notifications");
//        options.addArguments("--disable-gpu");
//        options.addArguments("--ignore-certificate-errors");
//        options.addArguments("--allow-running-insecure-content");
//        options.addArguments("--disable-web-security");
//        options.addArguments("--lang=en-US,en;q=0.9");
//        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
//
//        System.out.println("linux system detected, using absolute path");
//        options.setBinary("/usr/bin/chromium-browser");
//
//        options.addArguments("--remote-debugging-port=9222");
//        options.addArguments("--disable-setuid-sandbox");
//        options.addArguments("--disable-gpu-sandbox");
//
//        // Add experimental options
//        Map<String, Object> prefs = new HashMap<>();
//        prefs.put("profile.default_content_setting_values.notifications", 2);
//        prefs.put("credentials_enable_service", false);
//        prefs.put("profile.password_manager_enabled", false);
//        options.setExperimentalOption("prefs", prefs);
//        options.setExperimentalOption("excludeSwitches", Arrays.asList(
//                "enable-automation",
//                "disable-popup-blocking"
//        ));
//
//        try {
//            System.setProperty("webdriver.chrome.driver", "/usr/bin/chromedriver");
//
//            WebDriver driver = new ChromeDriver(options);
//            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
//            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
//
//            return driver;
//        } catch (Exception e) {
//            System.err.println("Failed to initialize driver on " + os + ": " + e.getMessage());
//            e.printStackTrace();
//            throw e;
//        }
//    }
//
//    private boolean attemptLoginWithCookies(WebDriver driver, WebDriverWait wait) {
//        try {
//            driver.get("https://www.keybr.com/");
//            boolean cookiesLoaded = CookieManager.loadCookies(driver);
//            if (!cookiesLoaded) {
//                return false;
//            }
//
//            driver.navigate().refresh();
//            Thread.sleep(2000);
//
//            try {
//                wait.until(ExpectedConditions.elementToBeClickable(
//                        By.xpath("//span[text()='Practice']")
//                ));
//                return true;
//            } catch (Exception e) {
//                return false;
//            }
//        } catch (Exception e) {
//            System.err.println("Cookie login failed: " + e.getMessage());
//            return false;
//        }
//    }
//
//    private void waitForPageLoad(WebDriver driver, WebDriverWait wait) {
//        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
//                .executeScript("return document.readyState").equals("complete"));
//    }
//
//    private void waitForElementToBeReady(WebElement element, WebDriverWait wait) {
//        wait.until(driver -> {
//            try {
//                return element.isDisplayed() && element.isEnabled();
//            } catch (StaleElementReferenceException e) {
//                return false;
//            }
//        });
//    }
//
//    private WebElement waitForElementSafely(WebDriver driver, By locator, WebDriverWait wait) {
//        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
//        WebElement element = wait.until(ExpectedConditions.elementToBeClickable(locator));
//        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
//        return element;
//    }
//
//    private void loginWithGoogle(WebDriver driver, WebDriverWait wait) {
//        try {
//            new File("screenshots").mkdirs();
//
//            if (attemptLoginWithCookies(driver, wait)) {
//                System.out.println("Successfully logged in using saved cookies");
//                return;
//            }
//
//            System.out.println("Cookie login failed, proceeding with normal login...");
//            driver.get("https://www.keybr.com/");
//            System.out.println("Navigated to keybr.com");
//            waitForPageLoad(driver, wait);
//            takeScreenshot(driver, "1_homepage");
//
//            // Click sign-in button
//            By signInLocator = By.xpath("//span[text()='Sign-In']");
//            WebElement signInButton = waitForElementSafely(driver, signInLocator, wait);
//            clickElementWithJS(driver, signInButton);
//            System.out.println("Clicked Sign-In button");
//            takeScreenshot(driver, "2_after_signin_click");
//            Thread.sleep(2000);
//
//            // Click Google sign-in button
//            By googleSignInLocator = By.xpath("//button[contains(., 'Sign-in with Google')]");
//            WebElement googleSignInButton = wait.until(ExpectedConditions.elementToBeClickable(googleSignInLocator));
//            Thread.sleep(1000);
//            clickElementWithJS(driver, googleSignInButton);
//            System.out.println("Clicked Google sign-in button");
//            Thread.sleep(3000);  // Wait for popup or redirect
//
//            // Check if we're already on the Google login page
//            String currentUrl = driver.getCurrentUrl();
//            System.out.println("Current URL: " + currentUrl);
//
//            // If not on Google login page, try to switch to popup
//            if (!currentUrl.contains("accounts.google.com")) {
//                String mainWindow = driver.getWindowHandle();
//                Set<String> handles = driver.getWindowHandles();
//                System.out.println("Available windows: " + handles.size());
//
//                for (String handle : handles) {
//                    if (!handle.equals(mainWindow)) {
//                        driver.switchTo().window(handle);
//                        break;
//                    }
//                }
//            }
//
//            // Now handle the Google login form
//            takeScreenshot(driver, "3_google_login");
//            System.out.println("Current URL before email: " + driver.getCurrentUrl());
//
//            // Wait for and enter email
//            By emailInputLocator = By.cssSelector("input[type='email']");
//            wait.until(ExpectedConditions.elementToBeClickable(emailInputLocator));
//            WebElement emailInput = driver.findElement(emailInputLocator);
//            emailInput.clear();
//            emailInput.sendKeys(googleEmail);
//            Thread.sleep(1000);
//            emailInput.sendKeys(Keys.ENTER);
//            System.out.println("Entered email");
//            takeScreenshot(driver, "4_after_email");
//
//            // Wait for and enter password
//            Thread.sleep(2000);
//            By passwordInputLocator = By.cssSelector("input[type='password']");
//            wait.until(ExpectedConditions.elementToBeClickable(passwordInputLocator));
//            WebElement passwordInput = driver.findElement(passwordInputLocator);
//            passwordInput.clear();
//            passwordInput.sendKeys(googlePassword);
//            Thread.sleep(1000);
//            passwordInput.sendKeys(Keys.ENTER);
//            System.out.println("Entered password");
//            takeScreenshot(driver, "5_after_password");
//
//            // If we switched to a popup, switch back to main window
//            if (!currentUrl.contains("accounts.google.com")) {
//                driver.switchTo().window(driver.getWindowHandles().iterator().next());
//            }
//
//            // Wait for successful login
//            wait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.xpath("//span[text()='Practice']")));
//            System.out.println("Successfully logged in");
//
//        } catch (Exception e) {
//            System.err.println("Login failed: " + e.getMessage());
//            takeScreenshot(driver, "error_state");
//            throw new RuntimeException("Login failed: " + e.getMessage(), e);
//        }
//    }
//
//    private String getPracticeTime(WebDriver driver, WebDriverWait wait) {
//        try {
//            WebElement practiceButton = wait.until(ExpectedConditions.elementToBeClickable(
//                    By.xpath("//span[text()='Practice']")
//            ));
//            clickElementWithJS(driver, practiceButton);
//            System.out.println("Clicked Practice button");
//
//            Thread.sleep(2000);
//
//            WebElement dailyGoalText = wait.until(ExpectedConditions.presenceOfElementLocated(
//                    By.xpath("//span[contains(text(), 'Daily goal:')]")
//            ));
//
//            String dailyGoalValue = dailyGoalText.getText();
//            System.out.println("Found daily goal: " + dailyGoalValue);
//
//            String[] parts = dailyGoalValue.split("/");
//            if (parts.length == 2) {
//                String percentageStr = parts[0].replace("%", "").trim();
//                String totalMinutesStr = parts[1].replace("minutes", "").trim();
//
//                double percentage = Double.parseDouble(percentageStr);
//                double totalMinutes = Double.parseDouble(totalMinutesStr);
//                double minutesPracticed = (percentage / 100.0) * totalMinutes;
//
//                return String.format("Total goal: %.0f minutes, Completed: %.1f minutes (%.1f%%)",
//                        totalMinutes, minutesPracticed, percentage);
//            }
//
//            return "Could not parse practice time";
//        } catch (Exception e) {
//            throw new RuntimeException("Error getting practice time: " + e.getMessage(), e);
//        }
//    }
//
//    private void clickElementWithJS(WebDriver driver, WebElement element) {
//        try {
//            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
//        } catch (Exception e) {
//            System.err.println("JS click failed: " + e.getMessage());
//            element.click();
//        }
//    }
//
//    private void takeScreenshot(WebDriver driver, String filename) {
//        try {
//            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
//            Files.copy(screenshot.toPath(), new File("screenshots/" + filename + ".png").toPath(),
//                    StandardCopyOption.REPLACE_EXISTING);
//            System.out.println("Screenshot saved: " + filename + ".png");
//        } catch (Exception e) {
//            System.err.println("Failed to take screenshot: " + e.getMessage());
//        }
//    }
//
//    // Add this new helper method for handling stale elements
//    private WebElement waitForElementWithRetry(WebDriver driver, WebDriverWait wait, By locator, int maxAttempts) {
//        for (int attempt = 0; attempt < maxAttempts; attempt++) {
//            try {
//                WebElement element = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
//                wait.until(ExpectedConditions.elementToBeClickable(element));
//                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
//                // Try to interact with element to verify it's truly available
//                element.isEnabled();
//                return element;
//            } catch (StaleElementReferenceException e) {
//                if (attempt == maxAttempts - 1) throw e;
//                System.out.println("Stale element, retrying... Attempt " + (attempt + 1));
//                try {
//                    Thread.sleep(1000);
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                }
//            }
//        }
//        throw new RuntimeException("Element not found after " + maxAttempts + " attempts");
//    }
//
//    private void printPageSource(WebDriver driver) {
//        try {
//            System.out.println("Current URL: " + driver.getCurrentUrl());
//            System.out.println("Page source preview: " +
//                    driver.getPageSource().substring(0, Math.min(1000, driver.getPageSource().length())));
//        } catch (Exception e) {
//            System.out.println("Failed to print page source: " + e.getMessage());
//        }
//    }
//}