# 评测与验证说明

## 一、文档目的
本文档记录当前项目的验证矩阵、执行命令、结果、缺口与后续建议，是验收与回归的直接依据。

## 二、验证矩阵
### 1. 工程结构
- 根文档存在：通过
- docs 详细文档存在：通过
- Git 仓库与远端存在：通过

### 2. 配置与安全
- `.env.example` 存在：通过
- MySQL 配置走环境变量：通过
- AI Key 未提交：通过
- `.gitignore` 覆盖依赖、构建产物、本地配置：通过

### 3. 数据模型与数据库
- `sql/mysql/init.sql` 可从空库导入：通过
- 核心表存在：通过
- 组织、角色、菜单、用户、题库、考试、答卷、成绩、事件种子数据存在：通过
- 系统以 MySQL 模式启动后关键接口可读取数据库：通过

### 4. 后端验证
- `mvn -q -DskipTests compile`：通过
- `mvn -q test`：通过
- `mvn -q -DskipTests package`：通过

### 5. 前端验证
- `npm.cmd run build`：通过
- 当前仍有 chunk size warning：已记录

### 6. API Smoke
已验证：
- `/api/auth/login`
- `/api/system/organizations`
- `/api/system/users`
- `/api/system/audit-logs`
- `/api/exam/questions/export`
- `/api/exam/candidate/my-exams`
- `/api/exam/candidate/exams/1?examPassword=MIDTERM2026`
- `/api/exam/analytics/overview`

## 三、数据库回归验证结果
### 步骤
1. 删除并重建 `exam_system`
2. 导入 `sql/mysql/init.sql`
3. 检查核心表与种子数据
4. 使用 MySQL 模式启动后端
5. 执行关键接口 smoke

### 当前结果
- `biz_organization = 3`
- `sys_user = 7`
- `sys_role = 6`
- `sys_menu = 18`
- `biz_question_bank = 4`
- `biz_exam_paper = 1`
- `biz_exam_plan = 2`
- `biz_answer_sheet = 1`
- `biz_answer_item = 4`
- `biz_score_record = 1`
- `biz_anti_cheat_event = 1`

## 四、仍未覆盖的验证
- 浏览器级 E2E
- 权限矩阵自动化测试
- 更细粒度接口自动化回归
- 压测与并发验证
