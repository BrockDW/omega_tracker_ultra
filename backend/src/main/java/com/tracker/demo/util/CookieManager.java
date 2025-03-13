package com.tracker.demo.util;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class CookieManager {
    private static final String COOKIES_FILE = "browser_cookies.txt";

    public static void saveCookies(WebDriver driver) {
        try (FileWriter fileWriter = new FileWriter(COOKIES_FILE);
             BufferedWriter writer = new BufferedWriter(fileWriter)) {

            // Get cookies from keybr.com domain only
            driver.get("https://www.keybr.com");
            Set<Cookie> cookies = driver.manage().getCookies();

            SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

            for (Cookie cookie : cookies) {
                StringBuilder cookieString = new StringBuilder();
                cookieString.append(cookie.getName()).append(";")
                        .append(cookie.getValue()).append(";")
                        .append(cookie.getDomain() == null ? "" : cookie.getDomain()).append(";")
                        .append(cookie.getPath() == null ? "" : cookie.getPath()).append(";");

                if (cookie.getExpiry() != null) {
                    cookieString.append(formatter.format(cookie.getExpiry()));
                }
                cookieString.append(";")
                        .append(cookie.isSecure());

                writer.write(cookieString.toString());
                writer.newLine();
            }
            System.out.println("Cookies saved successfully");
        } catch (Exception e) {
            System.err.println("Error saving cookies: " + e.getMessage());
        }
    }

    public static boolean loadCookies(WebDriver driver) {
        try {
            File file = new File(COOKIES_FILE);
            if (!file.exists()) {
                System.out.println("No cookies file found");
                return false;
            }

            // First navigate to the site
            driver.get("https://www.keybr.com");

            boolean cookiesLoaded = false;
            SimpleDateFormat formatter = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");

            try (BufferedReader reader = new BufferedReader(new FileReader(COOKIES_FILE))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] cookieData = line.split(";");
                    if (cookieData.length >= 5) {
                        try {
                            Cookie.Builder cookieBuilder = new Cookie.Builder(cookieData[0], cookieData[1]);

                            // Only set domain if it's not empty and contains keybr.com
                            if (!cookieData[2].isEmpty() && cookieData[2].contains("keybr.com")) {
                                cookieBuilder.domain(cookieData[2]);
                            }

                            // Set path if present
                            if (!cookieData[3].isEmpty()) {
                                cookieBuilder.path(cookieData[3]);
                            }

                            // Set expiry if present
                            if (cookieData.length > 4 && !cookieData[4].isEmpty()) {
                                try {
                                    Date expiry = formatter.parse(cookieData[4]);
                                    cookieBuilder.expiresOn(expiry);
                                } catch (Exception e) {
                                    // Ignore parse errors for expiry
                                }
                            }

                            // Set secure flag
                            if (cookieData.length > 5 && Boolean.parseBoolean(cookieData[5])) {
                                cookieBuilder.isSecure(true);
                            }

                            Cookie cookie = cookieBuilder.build();
                            driver.manage().addCookie(cookie);
                            cookiesLoaded = true;
                        } catch (Exception e) {
                            System.err.println("Error adding cookie: " + e.getMessage());
                            // Continue with next cookie
                        }
                    }
                }
            }
            return cookiesLoaded;
        } catch (Exception e) {
            System.err.println("Error loading cookies: " + e.getMessage());
            return false;
        }
    }
}