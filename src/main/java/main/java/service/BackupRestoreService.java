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

        String host = ConfigLoader.get("auth.datasource.jdbcUrl", "localhost:3306").split("/")[2].split(":")[0];
        String user = ConfigLoader.get("auth.datasource.username", "root");
        String password = ConfigLoader.get("auth.datasource.password", "");

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

        String host = ConfigLoader.get("auth.datasource.jdbcUrl", "localhost:3306").split("/")[2].split(":")[0];
        String user = ConfigLoader.get("auth.datasource.username", "root");
        String password = ConfigLoader.get("auth.datasource.password", "");

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
        // TODO: Implement archival logic
        // Move enrollments/grades older than N years to archive tables
    }
}
