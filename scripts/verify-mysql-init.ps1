$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$sqlFile = Join-Path $repoRoot 'sql\mysql\init.sql'
$tempDb = 'exam_system_regression'

Write-Host "[verify-mysql-init] 使用临时数据库 $tempDb 进行初始化回归..."

mysql --host=127.0.0.1 --port=3306 --user=root --password=123456 -e "DROP DATABASE IF EXISTS $tempDb; CREATE DATABASE $tempDb DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
Get-Content -Raw $sqlFile | mysql --host=127.0.0.1 --port=3306 --user=root --password=123456 $tempDb

$result = mysql --host=127.0.0.1 --port=3306 --user=root --password=123456 -D $tempDb -N -e @"
SELECT 'menus', COUNT(*) FROM sys_menu
UNION ALL SELECT 'users', COUNT(*) FROM sys_user
UNION ALL SELECT 'configs', COUNT(*) FROM sys_config_item
UNION ALL SELECT 'questions', COUNT(*) FROM biz_question_bank
UNION ALL SELECT 'papers', COUNT(*) FROM biz_exam_paper
UNION ALL SELECT 'plans', COUNT(*) FROM biz_exam_plan
UNION ALL SELECT 'records', COUNT(*) FROM biz_score_record
UNION ALL SELECT 'appeals', COUNT(*) FROM biz_score_appeal
UNION ALL SELECT 'login_risks', COUNT(*) FROM biz_login_risk_log
UNION ALL SELECT 'user_security_columns', COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = '$tempDb'
  AND TABLE_NAME = 'sys_user'
  AND COLUMN_NAME IN ('session_version', 'login_fail_count', 'last_login_failure_at', 'lock_until')
UNION ALL SELECT 'score_governance_columns', COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = '$tempDb'
  AND TABLE_NAME = 'biz_score_record'
  AND COLUMN_NAME IN ('review_status', 'appeal_status');
"@

Write-Host $result

$checks = @{}
foreach ($line in $result -split "`r?`n") {
  if ([string]::IsNullOrWhiteSpace($line)) { continue }
  $parts = $line -split "\s+"
  $checks[$parts[0]] = [int]$parts[1]
}

if ($checks['menus'] -lt 23) { throw "菜单数量异常：$($checks['menus'])" }
if ($checks['users'] -lt 74) { throw "用户数量异常：$($checks['users'])" }
if ($checks['configs'] -lt 28) { throw "配置项数量异常：$($checks['configs'])" }
if ($checks['questions'] -lt 320) { throw "题目数量异常：$($checks['questions'])" }
if ($checks['papers'] -lt 8) { throw "试卷数量异常：$($checks['papers'])" }
if ($checks['plans'] -lt 6) { throw "考试数量异常：$($checks['plans'])" }
if ($checks['records'] -lt 12) { throw "成绩记录数量异常：$($checks['records'])" }
if ($checks['appeals'] -lt 0) { throw "申诉表校验异常：$($checks['appeals'])" }
if ($checks['login_risks'] -lt 4) { throw "登录风险种子数量异常：$($checks['login_risks'])" }
if ($checks['user_security_columns'] -lt 4) { throw "用户安全字段缺失：$($checks['user_security_columns'])" }
if ($checks['score_governance_columns'] -lt 2) { throw "成绩治理字段缺失：$($checks['score_governance_columns'])" }

mysql --host=127.0.0.1 --port=3306 --user=root --password=123456 -e "DROP DATABASE IF EXISTS $tempDb;"

Write-Host "[verify-mysql-init] 初始化回归通过。"
