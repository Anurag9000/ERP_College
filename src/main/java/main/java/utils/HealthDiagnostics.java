package main.java.utils;

import main.java.config.ConfigLoader;
import main.java.config.DataSourceRegistry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Utility that executes application health diagnostics (datasource pings, config sanity checks).
 */
public final class HealthDiagnostics {
    private HealthDiagnostics() {
    }

    public static List<CheckResult> runAll() {
        List<CheckResult> results = new ArrayList<>();
        results.add(checkDataSource("Auth datasource", DataSourceRegistry.authDataSource()));
        results.add(checkDataSource("ERP datasource", DataSourceRegistry.erpDataSource()));
        results.add(checkConfigInteger("Security max failed attempts", "security.maxFailedAttempts"));
        results.add(checkConfigInteger("Registration max credits", "registration.maxCredits"));
        results.add(checkSessionTimeout());
        return results;
    }

    private static CheckResult checkDataSource(String name, Optional<DataSource> dataSource) {
        if (dataSource.isEmpty()) {
            return CheckResult.failure(name, "Datasource not configured.");
        }
        try (Connection connection = dataSource.get().getConnection()) {
            if (connection == null) {
                return CheckResult.failure(name, "getConnection() returned null.");
            }
            boolean valid = connection.isValid(2);
            return valid
                    ? CheckResult.success(name, "Connection valid.")
                    : CheckResult.failure(name, "Connection returned isValid=false.");
        } catch (Exception ex) {
            return CheckResult.failure(name, ex.getMessage());
        }
    }

    private static CheckResult checkConfigInteger(String label, String key) {
        String value = ConfigLoader.get(key);
        if (value == null || value.isBlank()) {
            return CheckResult.failure(label, "Missing configuration key: " + key);
        }
        try {
            Integer.parseInt(value.trim());
            return CheckResult.success(label, "Value=" + value.trim());
        } catch (NumberFormatException ex) {
            return CheckResult.failure(label, "Invalid integer: " + value);
        }
    }

    private static CheckResult checkSessionTimeout() {
        String value = ConfigLoader.getOrDefault("app.session.timeout.minutes", "30");
        try {
            long minutes = Long.parseLong(value.trim());
            return minutes > 0
                    ? CheckResult.success("Session timeout", minutes + " minutes")
                    : CheckResult.failure("Session timeout", "Value must be positive (currently " + minutes + ").");
        } catch (NumberFormatException ex) {
            return CheckResult.failure("Session timeout", "Invalid number: " + value);
        }
    }

    public static final class CheckResult {
        private final String name;
        private final boolean success;
        private final String details;

        private CheckResult(String name, boolean success, String details) {
            this.name = name;
            this.success = success;
            this.details = details;
        }

        public String getName() {
            return name;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getDetails() {
            return details;
        }

        private static CheckResult success(String name, String details) {
            return new CheckResult(name, true, details);
        }

        private static CheckResult failure(String name, String details) {
            return new CheckResult(name, false, details);
        }
    }
}
