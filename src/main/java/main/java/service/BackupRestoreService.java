package main.java.service;

import main.java.config.ConfigLoader;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for database backup and restore operations
 */
public class BackupRestoreService {

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
    public static void restoreBackup(String authBackupFile, String erpBackupFile)
            throws IOException, InterruptedException {

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

    private static void backupDatabase(String dbName, String outputFile)
            throws IOException, InterruptedException {

        String host = parseHostFromUrl(
                ConfigLoader.getOrDefault("auth.datasource.jdbcUrl", "jdbc:mysql://localhost:3306/college_auth"));
        String user = ConfigLoader.getOrDefault("auth.datasource.username", "root");
        String password = ConfigLoader.getOrDefault("auth.datasource.password", "");

        validateToolExists("mysqldump");

        ProcessBuilder pb = new ProcessBuilder(

                "mysqldump",
                "-h", host,
                "-u", user,
                "-p" + password,
                "--databases", dbName,
                "--result-file=" + outputFile);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Backup failed with exit code: " + exitCode);
        }
    }

    private static void restoreDatabase(String dbName, String inputFile)
            throws IOException, InterruptedException {

        String host = parseHostFromUrl(
                ConfigLoader.getOrDefault("auth.datasource.jdbcUrl", "jdbc:mysql://localhost:3306/college_auth"));
        String user = ConfigLoader.getOrDefault("auth.datasource.username", "root");
        String password = ConfigLoader.getOrDefault("auth.datasource.password", "");

        validateToolExists("mysql");

        ProcessBuilder pb = new ProcessBuilder(

                "mysql",
                "-h", host,
                "-u", user,
                "-p" + password,
                dbName);

        pb.redirectInput(new File(inputFile));
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Restore failed with exit code: " + exitCode);
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
        System.out.println("Archiving data older than " + cutoff);
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
