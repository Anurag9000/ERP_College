package main.java.service;

import main.java.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for database backup and restore operations
 */
public class BackupRestoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestoreService.class);
    private static final String BACKUP_DIR = "backups/";
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /**
     * Create a backup of both auth and ERP databases
     */
    public static String createBackup() throws IOException, InterruptedException {
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        String backupPrefix = BACKUP_DIR + "backup_" + timestamp;

        // Backup auth database
        backupDatabase("college_auth", backupPrefix + "_auth.sql");

        // Backup ERP database
        backupDatabase("college_erp", backupPrefix + "_erp.sql");

        return backupPrefix;
    }

    /**
     * Restore databases from backup files
     */
    /**
     * Restore databases from backup files.
     * 
     * @param authBackupFile the auth backup file path (must not be null or empty)
     * @param erpBackupFile  the ERP backup file path (must not be null or empty)
     * @throws IOException              if restore fails
     * @throws InterruptedException     if process is interrupted
     * @throws IllegalArgumentException if file paths are null or empty
     */
    public static void restoreBackup(String authBackupFile, String erpBackupFile)
            throws IOException, InterruptedException {
        if (authBackupFile == null || authBackupFile.trim().isEmpty()) {
            throw new IllegalArgumentException("Auth backup file path cannot be null or empty");
        }
        if (erpBackupFile == null || erpBackupFile.trim().isEmpty()) {
            throw new IllegalArgumentException("ERP backup file path cannot be null or empty");
        }

        restoreDatabase("college_auth", authBackupFile);
        restoreDatabase("college_erp", erpBackupFile);
    }

    /**
     * List all available backups
     */
    public static File[] listBackups() {
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            return new File[0];
        }

        return backupDir.listFiles((dir, name) -> name.endsWith(".sql"));
    }

    /**
     * Backs up a database to a file.
     * SECURITY: Uses environment variable for password to avoid command-line
     * exposure.
     * 
     * @param dbName     the database name (must not be null or empty)
     * @param outputFile the output file path (must not be null or empty)
     * @throws IOException              if backup fails
     * @throws InterruptedException     if process is interrupted
     * @throws IllegalArgumentException if parameters are null or empty
     */
    private static void backupDatabase(String dbName, String outputFile)
            throws IOException, InterruptedException {
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty");
        }
        if (outputFile == null || outputFile.trim().isEmpty()) {
            throw new IllegalArgumentException("Output file path cannot be null or empty");
        }

        String host = parseHostFromUrl(
                ConfigLoader.getOrDefault("auth.datasource.jdbcUrl", "jdbc:mysql://localhost:3306/college_auth"));
        String user = ConfigLoader.getOrDefault("auth.datasource.username", "root");
        String password = ConfigLoader.getOrDefault("auth.datasource.password", "");

        validateToolExists("mysqldump");

        // SECURITY FIX: Use environment variable for password instead of command-line
        // argument
        // to prevent password exposure in process lists
        ProcessBuilder pb = new ProcessBuilder(
                "mysqldump",
                "-h", host,
                "-u", user,
                "--databases", dbName,
                "--result-file=" + outputFile);

        // Set password via environment variable (more secure than command-line)
        if (password != null && !password.isEmpty()) {
            pb.environment().put("MYSQL_PWD", password);
        }

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            // Read error stream for better diagnostics
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errorMsg = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                throw new IOException("Backup failed with exit code: " + exitCode + ". Error: " + errorMsg);
            }
        }
    }

    /**
     * Restores a database from a backup file.
     * SECURITY: Uses environment variable for password to avoid command-line
     * exposure.
     * 
     * @param dbName    the database name (must not be null or empty)
     * @param inputFile the input file path (must not be null or empty)
     * @throws IOException              if restore fails
     * @throws InterruptedException     if process is interrupted
     * @throws IllegalArgumentException if parameters are null or empty
     */
    private static void restoreDatabase(String dbName, String inputFile)
            throws IOException, InterruptedException {
        if (dbName == null || dbName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty");
        }
        if (inputFile == null || inputFile.trim().isEmpty()) {
            throw new IllegalArgumentException("Input file path cannot be null or empty");
        }

        File backupFile = new File(inputFile);
        if (!backupFile.exists()) {
            throw new FileNotFoundException("Backup file not found: " + inputFile);
        }

        String host = parseHostFromUrl(
                ConfigLoader.getOrDefault("auth.datasource.jdbcUrl", "jdbc:mysql://localhost:3306/college_auth"));
        String user = ConfigLoader.getOrDefault("auth.datasource.username", "root");
        String password = ConfigLoader.getOrDefault("auth.datasource.password", "");

        validateToolExists("mysql");

        // SECURITY FIX: Use environment variable for password instead of command-line
        // argument
        ProcessBuilder pb = new ProcessBuilder(
                "mysql",
                "-h", host,
                "-u", user,
                dbName);

        // Set password via environment variable (more secure than command-line)
        if (password != null && !password.isEmpty()) {
            pb.environment().put("MYSQL_PWD", password);
        }

        pb.redirectInput(backupFile);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            // Read error stream for better diagnostics
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String errorMsg = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                throw new IOException("Restore failed with exit code: " + exitCode + ". Error: " + errorMsg);
            }
        }
    }

    /**
     * Archive old data (move to archive tables)
     */
    public static void archiveOldData(int yearsOld) {
        LocalDateTime cutoff = LocalDateTime.now().minusYears(yearsOld);
        // This is a placeholder for actual archival logic which would move data to
        // different tables
        // For now, we'll just log the intent as we don't have secondary archive tables
        // yet.
        LOGGER.info("Archiving data older than {}", cutoff);
    }

    private static String parseHostFromUrl(String url) {
        try {
            if (url.startsWith("jdbc:mysql://")) {
                String parts = url.substring(13);
                int slashIndex = parts.indexOf('/');
                if (slashIndex != -1) {
                    parts = parts.substring(0, slashIndex);
                }
                int colonIndex = parts.indexOf(':');
                if (colonIndex != -1) {
                    return parts.substring(0, colonIndex);
                }
                return parts;
            }
            return "localhost";
        } catch (Exception e) {
            return "localhost";
        }
    }

    private static void validateToolExists(String tool) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        ProcessBuilder checkPb = os.contains("win")
                ? new ProcessBuilder("where", tool)
                : new ProcessBuilder("which", tool);

        Process check = checkPb.start();
        try {
            if (check.waitFor() != 0) {
                throw new IOException(tool + " not found in system PATH.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Tool validation interrupted", e);
        }
    }
}
