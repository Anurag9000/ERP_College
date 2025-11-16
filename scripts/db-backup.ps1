param(
    [string]$OutputDirectory,
    [switch]$Help
)

function Show-Help {
    @"
Usage: .\db-backup.ps1 [-OutputDirectory <folder>] [ -Help ]

Environment variables (with defaults):
  DB_HOST (localhost)
  DB_PORT (3306)
  DB_USER (root)
  DB_PASSWORD (empty)
  AUTH_DB (college_auth)
  ERP_DB  (college_erp)
"@
}

if ($Help) {
    Show-Help
    exit 0
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
if (-not $OutputDirectory) {
    $OutputDirectory = Join-Path 'backups' $timestamp
}

if (-not (Test-Path $OutputDirectory)) {
    New-Item -ItemType Directory -Path $OutputDirectory | Out-Null
}

$env:DB_HOST = $env:DB_HOST ?? 'localhost'
$env:DB_PORT = $env:DB_PORT ?? '3306'
$env:DB_USER = $env:DB_USER ?? 'root'
$env:AUTH_DB = $env:AUTH_DB ?? 'college_auth'
$env:ERP_DB = $env:ERP_DB ?? 'college_erp'

function Backup-Schema {
    param([string]$Schema)
    $target = Join-Path $OutputDirectory ("{0}_{1}.sql" -f $Schema, $timestamp)
    Write-Host "Backing up $Schema -> $target"
    $originalPwd = $env:MYSQL_PWD
    if ($env:DB_PASSWORD) { $env:MYSQL_PWD = $env:DB_PASSWORD }
    & mysqldump -h $env:DB_HOST -P $env:DB_PORT -u $env:DB_USER $Schema 1> $target
    $env:MYSQL_PWD = $originalPwd
}

Backup-Schema $env:AUTH_DB
Backup-Schema $env:ERP_DB

Write-Host "Backup complete -> $OutputDirectory"
