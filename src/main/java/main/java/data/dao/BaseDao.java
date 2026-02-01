package main.java.data.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Base DAO providing connection helpers.
 */
public abstract class BaseDao {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final DataSource dataSource;

    protected BaseDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            String daoName = getClass().getSimpleName();
            throw new SQLException("CRITICAL: DataSource is not configured for " + daoName +
                    ". Verify your application.properties settings (jdbcUrl, username, password).");
        }
        Connection conn = dataSource.getConnection();
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Failed to obtain a valid database connection.");
        }
        return conn;
    }
}
