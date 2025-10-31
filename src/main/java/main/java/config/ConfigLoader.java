package main.java.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads application configuration from {@code application.properties}.
 */
public final class ConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String DEFAULT_RESOURCE = "/application.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = ConfigLoader.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in != null) {
                PROPERTIES.load(in);
            } else {
                LOGGER.warn("Configuration file {} not found on classpath.", DEFAULT_RESOURCE);
            }
        } catch (IOException e) {
            LOGGER.error("Unable to load configuration from {}", DEFAULT_RESOURCE, e);
        }
    }

    private ConfigLoader() {
    }

    public static String get(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String getOrDefault(String key, String defaultValue) {
        return PROPERTIES.getProperty(key, defaultValue);
    }

    public static Properties snapshot() {
        Properties copy = new Properties();
        copy.putAll(PROPERTIES);
        return copy;
    }
}
