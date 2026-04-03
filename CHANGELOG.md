# Changelog

## [Unreleased]

### 新增
- 新增试卷蓝图配置字段：版本、备注、乱序、题型分布、难度分布
- 新增教师端一体化试卷工作台
- 新增教师端“新建考试”语义化发布弹窗
- 新增学生端沉浸式考试态
- 新增防作弊事件字段：`leave_count`、`triggered_auto_save`、`save_version`
- 新增端到端用例 `teacher-exam-workflow.spec.ts`

### 变更
- 修复学生端倒计时逻辑，改为按“实际剩余作答时间”倒计时
- 修复考试时间窗口语义，明确区分“允许进入窗口”和“作答截止时间”
- 修复策略组卷仅能打开界面、不能实际组卷的问题
- 修复监考页仅能看到基础字段、无法关联考生和自动保存信息的问题
- 同步更新 MySQL 初始化脚本、运行时 schema/data 与根目录交付文档

### 验证
- 后端 `mvn -q test`
- 后端 `mvn -q -DskipTests package`
- 前端 `npm.cmd run build`
- 前端 `npm.cmd run test:e2e`
- MySQL 空库重建并导入 `sql/mysql/init.sql`
- 教师策略组卷保存试卷
- 教师发布测试考试
- 学生进入考试并触发防作弊事件
- 管理员查看监考事件
