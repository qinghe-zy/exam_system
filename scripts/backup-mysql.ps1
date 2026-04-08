param(
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Username = 'root',
    [string]$Password = '123456',
    [string]$Database = 'exam_system',
    [string]$OutputDir = ''
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputDir)) {
    $OutputDir = Join-Path $repoRoot 'database\backups'
}

$dumpCommand = Get-Command mysqldump -ErrorAction SilentlyContinue
if (-not $dumpCommand) {
    throw '未找到 mysqldump，请确认 MySQL Client 已安装并加入 PATH。'
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$outputFile = Join-Path $OutputDir "$Database-$timestamp.sql"

Write-Host "[backup-mysql] 开始备份数据库 $Database -> $outputFile"
& $dumpCommand.Source "--host=$DbHost" "--port=$Port" "--user=$Username" "--password=$Password" "--default-character-set=utf8mb4" "--single-transaction" "--routines" "--triggers" $Database | Set-Content -Path $outputFile -Encoding UTF8

if (-not (Test-Path $outputFile)) {
    throw "备份失败，未生成文件：$outputFile"
}

$file = Get-Item $outputFile
if ($file.Length -le 0) {
    throw "备份失败，文件为空：$outputFile"
}

Write-Host "[backup-mysql] 备份完成，文件大小：$([Math]::Round($file.Length / 1KB, 2)) KB"
Write-Host "[backup-mysql] 输出文件：$outputFile"
