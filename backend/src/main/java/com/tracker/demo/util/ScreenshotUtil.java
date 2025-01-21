package com.tracker.demo.util;


import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ScreenshotUtil {

    private final String screenshotPath;

    public ScreenshotUtil(String screenshotPath) {
        // E.g., "screenshots" or a path read from application.properties
        this.screenshotPath = screenshotPath;
        new File(this.screenshotPath).mkdirs();
    }

    public void takeScreenshot(WebDriver driver, String filename) {
        try {
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            File destination = new File(screenshotPath, filename + ".png");
            Files.copy(screenshot.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Screenshot saved: " + destination.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("Failed to take screenshot: " + e.getMessage());
        }
    }
}

