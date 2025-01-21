package com.tracker.demo.util;

import com.tracker.demo.config.ChromeProperties;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class WebDriverManager {

    private final ChromeProperties chromeProperties;

    // Constructor injection
    public WebDriverManager(ChromeProperties chromeProperties) {
        this.chromeProperties = chromeProperties;
    }

    public WebDriver createChromeDriver(boolean headless) {
        ChromeOptions options = new ChromeOptions();

        // 1. Set the binary path if provided
        String binaryPath = chromeProperties.getBinary();
        if (binaryPath != null && !binaryPath.isEmpty()) {
            options.setBinary(binaryPath);
        }

        // 2. Collect all arguments
        List<String> allArgs = new ArrayList<>();

        // additionalArgs
        List<String> additionalArgs = chromeProperties.getSettings().getAdditionalArgs();
        if (additionalArgs != null) {
            allArgs.addAll(additionalArgs);
        }

        // stealthArgs
        List<String> stealthArgs = chromeProperties.getSettings().getStealthArgs();
        if (stealthArgs != null) {
            allArgs.addAll(stealthArgs);
        }

        // userAgent
        String userAgent = chromeProperties.getSettings().getUserAgent();
        if (userAgent != null && !userAgent.isEmpty()) {
            allArgs.add("--user-agent=" + userAgent);
        }

        // headless
        if (headless) {
            // In recent Chrome, you can do "--headless=new"
            allArgs.add("--headless=new");
        }

        // Apply arguments
        options.addArguments(allArgs);

        // 3. If a driver path was specified, set it
        String driverPath = chromeProperties.getDriver();
        if (driverPath != null && !driverPath.isEmpty()) {
            System.setProperty("webdriver.chrome.driver", driverPath);
        }

        // 4. Create the driver
        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));

        // (Optional) debug logging
        System.out.println("Chrome binary: " + binaryPath);
        System.out.println("Chrome driver: " + driverPath);
        System.out.println("Arguments: " + allArgs);

        return driver;
    }
}
