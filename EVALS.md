# 评测与验证说明

## 一、文档目的
本文档用于记录当前项目的验证矩阵、执行命令、验证结果、未覆盖范围与后续建议，是当前阶段验收和回归的直接依据。

## 二、验证矩阵
### 1. 工程结构
- 根目录正式文档存在：通过
- `docs/` 详细文档目录存在：通过
- Git 仓库与远端存在：通过

### 2. 配置与安全
- `.env.example` 存在：通过
- MySQL 配置通过环境变量读取：通过
- AI Key 未提交：通过
- `.gitignore` 已覆盖依赖、构建产物、本地配置：通过

### 3. 数据模型与数据库
- `sql/mysql/init.sql` 可从空库完整导入：通过
- 核心表存在：通过
- 组织、角色、菜单、用户、题库、考试、答卷、成绩、站内消息、配置项、字典项、反作弊事件种子数据存在：通过
- MySQL 模式启动后关键接口能读取数据库：通过

### 4. 后端验证
- `mvn -q -DskipTests compile`：通过
- `mvn -q test`：通过
- `mvn -q -DskipTests package`：通过

### 5. 前端验证
- `npm.cmd run build`：通过
- 已完成路由懒加载与手工 vendor 拆包：通过
- 当前仍存在 `element-plus` vendor chunk 体积告警：已记录

### 6. 接口 smoke
已验证：
- `/api/auth/login`
- `/api/system/organizations`
- `/api/system/users`
- `/api/system/audit-logs`
- `/api/system/config-center/configs`
- `/api/system/config-center/dictionaries`
- `/api/messages/my`
- `/api/exam/questions/export`
- `/api/exam/candidate/my-exams`
- `/api/exam/candidate/exams/1?examPassword=MIDTERM2026`
- `/api/exam/analytics/overview`

### 7. 自动化测试
- 后端接口 smoke 回归：已通过
- 后端权限矩阵测试：已通过
- 浏览器级 E2E：已通过

## 三、数据库回归验证结果
### 验证步骤
1. 删除并重建 `exam_system`
2. 导入 `sql/mysql/init.sql`
3. 检查核心表与种子数据
4. 以 MySQL 模式启动后端
5. 执行关键接口 smoke

### 当前计数结果
- `biz_organization = 3`
- `sys_user = 7`
- `sys_role = 6`
- `sys_menu = 20`
- `biz_question_bank = 4`
- `biz_exam_paper = 1`
- `biz_exam_plan = 2`
- `biz_answer_sheet = 1`
- `biz_answer_item = 4`
- `biz_score_record = 1`
- `biz_in_app_message = 2`
- `sys_config_item = 3`
- `sys_dictionary_item = 8`
- `biz_anti_cheat_event = 1`

## 四、自动化测试说明
### 1. 后端接口 smoke
覆盖内容：
- 管理员读取组织、配置项、字典项、题库导出
- 学生读取消息、待考列表、考试工作区
- 管理员读取分析结果

### 2. 权限矩阵测试
覆盖角色：
- 管理员
- 组织管理员
- 教师
- 阅卷老师
- 学生

验证重点：
- 允许访问的接口返回成功
- 无权限接口返回 `403`
- 组织管理员看到的是本组织树而不是全量根树

### 3. 浏览器级 E2E
覆盖场景：
1. 学生登录 -> 进入考试 -> 作答 -> 提交
2. 阅卷老师登录 -> 打开阅卷任务 -> 评分 -> 提交

## 五、当前仍未覆盖的验证
- 更复杂的数据权限边界自动化场景
- 更大范围浏览器回归套件
- 压测与并发验证
- 外部通知通道集成验证
