# MySQL 备份与恢复 Runbook

## 一、文档目的
本文档用于指导本地开发环境和验收环境对 `exam_system` 数据库执行基础备份、恢复和恢复后校验，确保当前仓库不只具备“可初始化”，还具备“可回滚、可恢复、可验证”的基础运维能力。

## 二、适用范围
- 数据库类型：MySQL
- 默认连接：
  - Host：`127.0.0.1`
  - Port：`3306`
  - Username：`root`
  - Password：`123456`
- 适用数据库：
  - 正式本地开发库：`exam_system`
  - 恢复验证临时库：`exam_system_restore_verify`

## 三、前置条件
1. 已安装 MySQL Client，并确保以下命令可直接执行：
   - `mysql`
   - `mysqldump`
2. 当前仓库脚本可用：
   - `scripts/backup-mysql.ps1`
   - `scripts/restore-mysql.ps1`
3. 当前数据库账号具备建库、删库、导入、导出权限。

## 四、备份步骤
### 4.1 标准命令
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/backup-mysql.ps1
```

### 4.2 可选参数
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/backup-mysql.ps1 `
  -Database exam_system `
  -OutputDir database\backups
```

### 4.3 预期结果
1. 终端输出备份文件路径。
2. `database/backups/` 下生成以时间戳命名的 SQL 文件。
3. 文件大小大于 0 KB。

## 五、恢复步骤
### 5.1 恢复到验证库
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/restore-mysql.ps1 `
  -BackupFile database\backups\exam_system-20260408-193000.sql `
  -Database exam_system_restore_verify `
  -DropExisting
```

### 5.2 预期结果
1. 成功创建目标数据库。
2. 导入完成后终端输出：
   - 表数量
   - 菜单数量
   - 用户数量
   - 配置项数量
3. 无 SQL 执行错误。

## 六、恢复后校验
建议至少执行以下校验：
1. 结构校验
   - `sys_user` 是否存在 `login_fail_count`、`last_login_failure_at`、`lock_until`
   - `biz_login_risk_log` 是否存在
2. 数据校验
   - `sys_menu` 至少 23 条
   - `sys_config_item` 至少 21 条
   - `biz_question_bank` 至少 320 条
3. 自动化校验
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-mysql-init.ps1
```

## 七、失败处理
### 7.1 `mysqldump` 不存在
- 现象：脚本直接报错“未找到 mysqldump”
- 处理：
  1. 安装 MySQL Client
  2. 将 MySQL `bin` 目录加入 PATH
  3. 重新打开终端后重试

### 7.2 恢复时报编码或 SQL 错误
- 现象：恢复中断，终端出现 SQL 解析失败
- 处理：
  1. 确认备份文件由本仓库脚本生成
  2. 确认目标库字符集为 `utf8mb4`
  3. 重新执行恢复命令并开启 `-DropExisting`

### 7.3 恢复后数量明显不足
- 现象：表存在，但菜单、配置项、题目数量偏少
- 处理：
  1. 优先执行 `scripts/verify-mysql-init.ps1`
  2. 若验证失败，重新导入 `sql/mysql/init.sql`
  3. 比对最近一次备份是否来自完整环境

## 八、当前结论
当前仓库已经补齐基础备份脚本、恢复脚本和恢复后结构校验方法，满足“本地开发与验收环境可恢复”的基础交付要求。
