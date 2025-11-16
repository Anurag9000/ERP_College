param(
    [Parameter(Mandatory=$true)] [string]$BackupDirectory,
    [switch]$Help
)

function Show-Help {
    @"
Usage: .\db-restore.ps1 -BackupDirectory <folder>

Environment variables (with defaults):
  DB_HOST (localhost)
  DB_PORT (3306)
  DB_USER (root)
  DB_PASSWORD (empty)
"@
}

if ($Help) {
    Show-Help
    exit 0
}

if (-not (Test-Path $BackupDirectory)) {
    Write-Error "Backup directory '$BackupDirectory' not found."
    exit 1
}

$env:DB_HOST = $env:DB_HOST ?? 'localhost'
$env:DB_PORT = $env:DB_PORT ?? '3306'
$env:DB_USER = $env:DB_USER ?? 'root'

function Restore-File {
    param([string]$File)
    $schema = (Split-Path $File -Leaf).Split('_')[0]
    Write-Host "Restoring $File -> $schema"
    $originalPwd = $env:MYSQL_PWD
    if ($env:DB_PASSWORD) { $env:MYSQL_PWD = $env:DB_PASSWORD }
    & mysql -h $env:DB_HOST -P $env:DB_PORT -u $env:DB_USER $schema 1< $File
    $env:MYSQL_PWD = $originalPwd
}

$sqlFiles = Get-ChildItem -Path $BackupDirectory -Filter *.sql
if (-not $sqlFiles) {
    Write-Error "No .sql files found in $BackupDirectory"
    exit 1
}

foreach ($file in $sqlFiles) {
    Restore-File $file.FullName
}

Write-Host "Restore complete."
