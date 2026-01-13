package edu.univ.erp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuration utility to load application properties.
 */
public class ConfigUtil {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtil.class);
    private static Properties properties;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        properties = new Properties();
        try (InputStream input = ConfigUtil.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Configuration loaded successfully");
            } else {
                logger.warn("application.properties not found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Failed to load configuration", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public static int getIntProperty(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.warn("Invalid integer value for key: {}", key);
            }
        }
        return defaultValue;
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    // Database Configuration
    public static String getAuthDbUrl() {
        return getProperty("auth.db.url", "jdbc:mysql://localhost:3306/erp_auth");
    }

    public static String getAuthDbUsername() {
        return getProperty("auth.db.username", "root");
    }

    public static String getAuthDbPassword() {
        return getProperty("auth.db.password", "password");
    }

    public static String getErpDbUrl() {
        return getProperty("erp.db.url", "jdbc:mysql://localhost:3306/erp_main");
    }

    public static String getErpDbUsername() {
        return getProperty("erp.db.username", "root");
    }

    public static String getErpDbPassword() {
        return getProperty("erp.db.password", "password");
    }
}
