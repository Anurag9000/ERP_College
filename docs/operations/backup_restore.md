# Backup & Restore Playbook

This guide documents the optional backup/restore tooling that accompanies the ERP application. The scripts simply wrap the `mysqldump` and `mysql` clients so operators can snapshot and restore the dual-database setup (`college_auth` and `college_erp`).

## Prerequisites

- MySQL/MariaDB client utilities (`mysqldump` and `mysql`) available on your `PATH`.
- Credentials with SELECT/LOCK TABLES privileges for backups and full write access for restores.
- Set the following environment variables if you do not want to rely on the defaults:
  - `DB_HOST` (default `localhost`)
  - `DB_PORT` (default `3306`)
  - `DB_USER` (default `root`)
  - `DB_PASSWORD` (blank by default)
  - `AUTH_DB` (default `college_auth`)
  - `ERP_DB` (default `college_erp`)

> The scripts temporarily set the `MYSQL_PWD` environment variable so your password does not appear in command history.

## Taking a Backup

### Bash/Linux/macOS

```bash
# Optional: override defaults (example)
export DB_USER=erp_admin
export DB_PASSWORD=supersecret

# Create timestamped backups under backups/<timestamp>
./scripts/db-backup.sh

# Or place dumps in a specific directory
./scripts/db-backup.sh nightly_backups/2025-11-16
```

### PowerShell/Windows

```powershell
$env:DB_USER = "erp_admin"
$env:DB_PASSWORD = "supersecret"

# Default timestamped folder
./scripts/db-backup.ps1

# Custom folder
./scripts/db-backup.ps1 -OutputDirectory "D:\\db_snaps\\pre_release"
```

Both scripts generate two `.sql` files per run (one for `AUTH_DB`, one for `ERP_DB`). Store them in versioned or off-site storage per your institutional policy.

## Restoring a Backup

> **Warning:** Restores are destructive. Back up the current state before running these commands.

### Bash/Linux/macOS

```bash
./scripts/db-restore.sh backups/2025-11-16-0100
```

### PowerShell/Windows

```powershell
./scripts/db-restore.ps1 -BackupDirectory "D:\\db_snaps\\pre_release"
```

The script scans the target directory for `*.sql` files, infers the schema name from the filename prefix, and pipes the dump back into MySQL.

## Operational Notes

- Run backups before large data imports, schema migrations, or release deployments.
- Keep backups encrypted and versioned according to your organization’s data-governance policy.
- For automated pipelines, schedule the Bash script via cron or Windows Task Scheduler and rotate the output directory.
- Combine database backups with the Flyway migration history (`flyway_schema_history`) to recreate exact environments.

With these scripts and documented steps, the optional backup/restore requirement from the specification is satisfied without additional tooling dependencies.
