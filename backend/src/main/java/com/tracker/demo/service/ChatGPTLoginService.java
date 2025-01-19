package com.tracker.demo.service;

import com.tracker.demo.util.CloudflareAuthHandler;
import com.tracker.demo.util.CloudflareTurnstileHandler;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Arrays;

// TODO: This class is deprecated, will modify it later

@Service
public class ChatGPTLoginService {

    @Value("${chatgpt.email}")
    private String chatgptEmail;

    @Value("${chatgpt.password}")
    private String chatgptPassword;

    // If you're on a Pi or server that doesn't have a visible display, use headless:
    public String loginAndGetPageContent() {
        // 1) Setup ChromeOptions for headless mode
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-extensions");
        options.addArguments("--no-sandbox");
        options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));

        // 2) Create the ChromeDriver (opens the browser in memory)
        WebDriver driver = new ChromeDriver(options);

        ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined});"
        );

        try {
            // 3) Navigate to chat.openai.com
            driver.get("https://chat.openai.com/");

            // 4) Wait for the "Log in" button and click it
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            Thread.sleep(5000);

            CloudflareTurnstileHandler turnstileHandler = new CloudflareTurnstileHandler(driver);

// Debug the page first
            turnstileHandler.debugPage();

// Then handle any Cloudflare challenges
            turnstileHandler.handleTurnstile();

            Thread.sleep(5000);

            captureScreenshot(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE));

            WebElement loginButton = wait.until(
                    ExpectedConditions.elementToBeClickable(By.xpath("//button[.//div[text()='Log in']]"))
            );

            loginButton.click();

            // 5) Wait for the email input
            WebElement emailInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.name("username"))
            );
            // Type the email
            emailInput.sendKeys(chatgptEmail);

            // 6) Click the "Continue" button
            WebElement continueBtn = driver.findElement(By.xpath("//button[contains(text(),'Continue')]"));
            continueBtn.click();

            // 7) Wait for the password input
            WebElement passwordInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.name("password"))
            );
            // Type the password
            passwordInput.sendKeys(chatgptPassword);

            // 8) Click "Continue"
            WebElement submitBtn = driver.findElement(By.xpath("//button[contains(text(),'Continue')]"));
            submitBtn.click();

            // 9) Wait for the main chat page to load
            //    For example, wait for an element that only appears after login
            WebElement chatInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(By.tagName("textarea")) // or some other unique element
            );

            // 10) Get the entire page source after login
            String pageSource = driver.getPageSource();

            // Return the HTML as a string
            return pageSource;

        } catch (Exception e) {
            e.printStackTrace();
            return "Login failed or encountered an error: " + e.getMessage();
        } finally {
            // Always quit the driver
            driver.quit();
        }
    }

    public void captureScreenshot(File screenshotTmpFile) throws IOException {

        // 2) Define the target file path (e.g., C:\temp\screenshot.png)
        Path targetPath = Path.of("C:", "temp", screenshotTmpFile.getName()); // or new File("C:\\temp\\screenshot.png").toPath()

        // 3) Copy from the temporary file to the target
        Files.copy(
                screenshotTmpFile.toPath(),
                targetPath,
                StandardCopyOption.REPLACE_EXISTING
        );

        System.out.println("Screenshot saved to: " + targetPath);
    }
}
