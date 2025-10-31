package main.java.config;

import java.util.Objects;

/**
 * Immutable representation of a JDBC data source configuration.
 */
public final class DatabaseProperties {
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final int maximumPoolSize;

    private DatabaseProperties(Builder builder) {
        this.jdbcUrl = Objects.requireNonNull(builder.jdbcUrl, "jdbcUrl");
        this.username = Objects.requireNonNull(builder.username, "username");
        this.password = Objects.requireNonNull(builder.password, "password");
        this.maximumPoolSize = builder.maximumPoolSize;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String jdbcUrl;
        private String username;
        private String password;
        private int maximumPoolSize = 5;

        private Builder() {
        }

        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public DatabaseProperties build() {
            return new DatabaseProperties(this);
        }
    }
}
