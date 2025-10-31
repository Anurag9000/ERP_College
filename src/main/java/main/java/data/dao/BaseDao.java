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

    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
