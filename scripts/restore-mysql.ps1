param(
    [Parameter(Mandatory = $true)]
    [string]$BackupFile,
    [string]$DbHost = '127.0.0.1',
    [int]$Port = 3306,
    [string]$Username = 'root',
    [string]$Password = '123456',
    [string]$Database = 'exam_system_restore_verify',
    [switch]$DropExisting
)

$ErrorActionPreference = 'Stop'

if (-not (Test-Path $BackupFile)) {
    throw "备份文件不存在：$BackupFile"
}

$mysqlCommand = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysqlCommand) {
    throw '未找到 mysql 客户端，请确认 MySQL Client 已安装并加入 PATH。'
}

if ($DropExisting) {
    Write-Host "[restore-mysql] 删除已有数据库：$Database"
    & $mysqlCommand.Source "--host=$DbHost" "--port=$Port" "--user=$Username" "--password=$Password" -e "DROP DATABASE IF EXISTS $Database;"
}

Write-Host "[restore-mysql] 创建数据库：$Database"
& $mysqlCommand.Source "--host=$DbHost" "--port=$Port" "--user=$Username" "--password=$Password" -e "CREATE DATABASE IF NOT EXISTS $Database DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

Write-Host "[restore-mysql] 开始导入：$BackupFile"
Get-Content -Raw $BackupFile | & $mysqlCommand.Source "--host=$DbHost" "--port=$Port" "--user=$Username" "--password=$Password" $Database

$result = & $mysqlCommand.Source "--host=$DbHost" "--port=$Port" "--user=$Username" "--password=$Password" -D $Database -N -e @"
SELECT 'tables', COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '$Database'
UNION ALL SELECT 'menus', COUNT(*) FROM sys_menu
UNION ALL SELECT 'users', COUNT(*) FROM sys_user
UNION ALL SELECT 'configs', COUNT(*) FROM sys_config_item;
"@

Write-Host "[restore-mysql] 恢复结果："
Write-Host $result
