# MySQL 初始化与回归说明

## 一、适用范围
用于从空数据库开始构建当前系统的正式交付数据库，并验证系统能在 MySQL 模式下正常读取数据。

## 二、目标数据库
- 数据库名：`exam_system`
- Host：`127.0.0.1`
- Port：`3306`
- Username：`root`
- Password：本地开发密码

## 三、脚本口径
- 全量初始化脚本：`sql/mysql/init.sql`
- 当前安全增强已新增用户登录安全字段：
  - `sys_user.login_fail_count`
  - `sys_user.last_login_failure_at`
  - `sys_user.lock_until`
- 当前无独立增量修复脚本
- 若后续新增增量脚本，必须单独补充执行顺序与适用条件说明
- 当前已提供临时库回归脚本：`scripts/verify-mysql-init.ps1`
- 当前已提供备份脚本：`scripts/backup-mysql.ps1`
- 当前已提供恢复脚本：`scripts/restore-mysql.ps1`

## 四、回归步骤
1. 删除旧库
2. 创建新库
3. 使用文件重定向方式导入 `sql/mysql/init.sql`
4. 检查组织、用户、角色、菜单、题库、考试、消息、配置、字典、反作弊事件等核心数据
5. 使用 MySQL 模式启动后端
6. 执行关键接口 smoke
7. 如需自动化回归，可执行：`powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-mysql-init.ps1`

## 五、当前验证结果
- 学科字典：8
- 组织：12
- 用户：74
- 角色：6
- 菜单：23
- 题目：320
- 试卷：8
- 考试计划：6
- 答卷：12
- 答题项：48
- 成绩记录：12
- 站内消息：72
- 登录风险日志：4
- 系统参数：21
- 字典项：23
- 反作弊事件：12
- 用户安全字段：3（失败次数 / 最近失败时间 / 锁定截止时间）

## 六、结论
当前初始化脚本满足“从空库完整重建”的验收要求。
