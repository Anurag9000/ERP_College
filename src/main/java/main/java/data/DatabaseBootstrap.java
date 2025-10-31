package main.java.data;

import main.java.config.DataSourceRegistry;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Runs database migrations for Auth and ERP schemas.
 */
public final class DatabaseBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseBootstrap.class);

    private DatabaseBootstrap() {
    }

    public static void migrate() {
        migrateDataSource(DataSourceRegistry.authDataSource().orElse(null), "db/auth");
        migrateDataSource(DataSourceRegistry.erpDataSource().orElse(null), "db/erp");
    }

    private static void migrateDataSource(DataSource dataSource, String location) {
        if (dataSource == null) {
            LOGGER.warn("Skipping Flyway migration for {} (datasource unavailable).", location);
            return;
        }
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:" + location)
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();
            LOGGER.info("Flyway migration completed for {}", location);
        } catch (Exception ex) {
            LOGGER.error("Flyway migration failed for {}: {}", location, ex.getMessage(), ex);
            throw ex;
        }
    }
}
