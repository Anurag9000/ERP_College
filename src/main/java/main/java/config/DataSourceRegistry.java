package main.java.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralised registry for application data sources.
 */
public final class DataSourceRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceRegistry.class);
    private static final Map<String, HikariDataSource> DATA_SOURCES = new ConcurrentHashMap<>();

    public static final String AUTH_KEY = "auth";
    public static final String ERP_KEY = "erp";

    static {
        register(AUTH_KEY, loadProperties("auth"));
        register(ERP_KEY, loadProperties("erp"));
    }

    private DataSourceRegistry() {
    }

    private static DatabaseProperties loadProperties(String prefix) {
        String base = prefix + ".datasource.";
        String jdbcUrl = ConfigLoader.get(base + "jdbcUrl");
        String username = ConfigLoader.get(base + "username");
        String password = ConfigLoader.get(base + "password");
        String maxPool = ConfigLoader.get(base + "maximumPoolSize");

        if (jdbcUrl == null || username == null || password == null) {
            LOGGER.warn("Incomplete datasource configuration for prefix '{}'. JDBC operations will be unavailable.", prefix);
            return null;
        }

        int maxPoolSize = 5;
        try {
            if (maxPool != null) {
                maxPoolSize = Integer.parseInt(maxPool.trim());
            }
        } catch (NumberFormatException ex) {
            LOGGER.warn("Invalid maximumPoolSize for {}. Using default {}", prefix, maxPoolSize);
        }

        return DatabaseProperties.builder()
                .jdbcUrl(jdbcUrl)
                .username(username)
                .password(password)
                .maximumPoolSize(maxPoolSize)
                .build();
    }

    private static void register(String key, DatabaseProperties props) {
        if (props == null) {
            return;
        }
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(props.getJdbcUrl());
        config.setUsername(props.getUsername());
        config.setPassword(props.getPassword());
        config.setMaximumPoolSize(props.getMaximumPoolSize());
        config.setPoolName("ERP-" + key.toUpperCase() + "-POOL");
        config.setAutoCommit(false);
        config.setInitializationFailTimeout(-1L);

        HikariDataSource dataSource = new HikariDataSource(config);
        DATA_SOURCES.put(key, dataSource);
        LOGGER.info("Initialised {} datasource (maxPoolSize={})", key, props.getMaximumPoolSize());
    }

    public static Optional<DataSource> dataSource(String key) {
        return Optional.ofNullable(DATA_SOURCES.get(key));
    }

    public static Optional<DataSource> authDataSource() {
        return dataSource(AUTH_KEY);
    }

    public static Optional<DataSource> erpDataSource() {
        return dataSource(ERP_KEY);
    }

    public static void shutdownAll() {
        DATA_SOURCES.values().forEach(ds -> {
            try {
                ds.close();
            } catch (Exception e) {
                LOGGER.warn("Error closing datasource {}: {}", ds, e.getMessage());
            }
        });
        DATA_SOURCES.clear();
    }
}
