package com.tracker.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps the 'chrome' section of application.yml into this class.
 */
@Component
@ConfigurationProperties(prefix = "chrome")
public class ChromeProperties {

    private String binary;
    private String driver;
    private Settings settings;

    // Getters and setters
    public String getBinary() {
        return binary;
    }
    public void setBinary(String binary) {
        this.binary = binary;
    }

    public String getDriver() {
        return driver;
    }
    public void setDriver(String driver) {
        this.driver = driver;
    }

    public Settings getSettings() {
        return settings;
    }
    public void setSettings(Settings settings) {
        this.settings = settings;
    }

    /**
     * Nested class to map 'chrome.settings'
     */
    public static class Settings {
        private String userAgent;
        private List<String> additionalArgs;
        private List<String> stealthArgs;

        // Getters and setters
        public String getUserAgent() {
            return userAgent;
        }
        public void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }

        public List<String> getAdditionalArgs() {
            return additionalArgs;
        }
        public void setAdditionalArgs(List<String> additionalArgs) {
            this.additionalArgs = additionalArgs;
        }

        public List<String> getStealthArgs() {
            return stealthArgs;
        }
        public void setStealthArgs(List<String> stealthArgs) {
            this.stealthArgs = stealthArgs;
        }
    }
}

