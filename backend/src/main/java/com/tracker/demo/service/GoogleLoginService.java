package com.tracker.demo.service;

import com.tracker.demo.util.ScreenshotUtil;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Set;

public class GoogleLoginService {

    private final String googleEmail;
    private final String googlePassword;
    private final ScreenshotUtil screenshotUtil;

    public GoogleLoginService(String googleEmail, String googlePassword, ScreenshotUtil screenshotUtil) {
        this.googleEmail = googleEmail;
        this.googlePassword = googlePassword;
        this.screenshotUtil = screenshotUtil;
    }

    public void loginWithGoogle(WebDriver driver, String mainWindowTitle) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

        try {
            // Ensure we click the "Sign in with Google" button in the current page
            screenshotUtil.takeScreenshot(driver, "google-login-before");

            // The assumption here is that the method calling this code already clicked
            // whatever button triggers the Google sign-in popup or redirection
            // so now we handle the actual Google login flow.

            String currentUrl = driver.getCurrentUrl();
            // If not on Google login page, try to switch to popup
            if (!currentUrl.contains("accounts.google.com")) {
                switchToPopUpWindow(driver, mainWindowTitle);
            }

            screenshotUtil.takeScreenshot(driver, "google-login-popup");

            // Email
            By emailInputLocator = By.cssSelector("input[type='email']");
            wait.until(ExpectedConditions.elementToBeClickable(emailInputLocator)).sendKeys(googleEmail + Keys.ENTER);
            screenshotUtil.takeScreenshot(driver, "google-email-entered");

            // Password
            By passwordInputLocator = By.cssSelector("input[type='password']");
            wait.until(ExpectedConditions.elementToBeClickable(passwordInputLocator)).sendKeys(googlePassword + Keys.ENTER);
            screenshotUtil.takeScreenshot(driver, "google-password-entered");

            // Possibly switch back to main window
            driver.switchTo().window(mainWindowTitle);

            // Wait for successful sign-in – depends on your application flow
            wait.until(ExpectedConditions.urlContains("keybr.com"));
        } catch (Exception e) {
            screenshotUtil.takeScreenshot(driver, "google-login-error");
            throw new RuntimeException("Google login failed: " + e.getMessage(), e);
        }
    }

    private void switchToPopUpWindow(WebDriver driver, String mainWindow) {
        Set<String> handles = driver.getWindowHandles();
        for (String handle : handles) {
            if (!handle.equals(mainWindow)) {
                driver.switchTo().window(handle);
                return;
            }
        }
    }
}
