# 评测与验证说明

## 一、文档目的
本文档记录本轮“试卷组卷链路、考试时间语义、防作弊闭环、学生端沉浸式重构”的真实验证结果。

## 二、验证矩阵
### 1. 构建验证
- 后端 `mvn -q test`：通过
- 后端 `mvn -q -DskipTests package`：通过
- 前端 `npm.cmd run build`：通过

### 2. 数据库回归验证
已执行：
1. 删除并重建 `exam_system`
2. 导入 `sql/mysql/init.sql`
3. 校验新增字段与样例数据
4. 启动 MySQL 模式后端并执行接口回归

已确认：
- `biz_exam_paper` 新增：
  - `paper_version`
  - `remark_text`
  - `shuffle_enabled`
  - `question_type_config_json`
  - `difficulty_config_json`
- `biz_anti_cheat_event` 新增：
  - `leave_count`
  - `triggered_auto_save`
  - `save_version`

### 3. 关键账号登录 smoke
- 管理员：`900001 / 123456` 通过
- 教师：`800001 / 123456` 通过
- 学生：`20260001 / 123456` 通过

### 4. 接口 smoke
已验证：
- `POST /api/auth/login`
- `GET /api/exam/papers`
- `POST /api/exam/plans`
- `GET /api/exam/candidate/my-exams`
- `GET /api/exam/candidate/exams/{id}`
- `POST /api/exam/candidate/exams/{id}/events`
- `GET /api/exam/proctor/events`

## 三、浏览器级 E2E
### 1. 全量回归
- `npm.cmd run test:e2e`：通过（4 / 4）

### 2. 覆盖场景
1. 学生登录 -> 进入考试 -> 作答 -> 提交
2. 阅卷老师登录 -> 打开阅卷任务 -> 评分 -> 提交
3. 教师登录 -> 进入试卷管理 -> 手工取题 / 策略组卷 -> 打开考试发布页
4. 教师策略组卷并保存试卷 -> 创建考试 -> 学生进入后显示作答倒计时 -> 触发防作弊事件 -> 管理员查看监考事件

## 四、专项结果
### 1. 新建试卷与组卷
- 教师可在新版试卷工作台中完成：
  - 设置试卷名称、学科、组卷方式、版本、备注、时长、及格线、乱序与卷面说明
  - 从题库筛题并加入卷面
  - 调整顺序、分值、必答/选答
  - 执行随机组卷与策略组卷
- 实测新增 `AUTO-*` 策略试卷已落库：
  - `questionCount = 10`
  - `assemblyMode = STRATEGY`
  - `questionTypeConfigs` 与 `difficultyConfigs` 已写入

### 2. 新建考试与考试时间语义
- 已实测新增 `AUTO-KS-*` 考试计划落库成功
- 学生进入后显示：
  - 允许进入窗口：`04/03 11:00 至 04/03 14:00`
  - 本场剩余作答时间：`01:30:00`
  - 自动交卷时间：按 `startedAt + durationMinutes` 与 `endTime` 取最小值

### 3. 防作弊闭环
- 触发 `TAB_SWITCH` / `WINDOW_BLUR` / `FULLSCREEN_EXIT` 后，MySQL 中可见新增事件
- 新事件字段示例：
  - `leave_count = 1`
  - `triggered_auto_save = 1/0`
  - `save_version = 1/2/3`
- 对应答卷 `biz_answer_sheet.save_version` 已增加到 3，说明异常触发后自动保存已实际执行

### 4. 后台可查
- 管理员访问 `/exam/proctor` 页面可查询到监考事件
- 页面已显示：
  - 考试名称
  - 考生姓名
  - 事件类型
  - 严重级别
  - 累计次数
  - 是否自动保存
  - 答卷版本

## 五、AI 模块核查结果
- 结论：仅完成后端配置与适配层预留，未接入真实 AI 业务能力
- 已核实位置：
  - `.env.example`
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/java/com/projectexample/examsystem/infra/ai/AiGatewayProperties.java`
  - `backend/src/main/java/com/projectexample/examsystem/infra/ai/AiGatewayClient.java`

## 六、未完全覆盖项
- 真实人工切屏、窗口最小化、多屏、多设备场景仍建议补手测
- 更复杂的阅卷与成绩发布组合场景仍可继续扩大自动化覆盖
- 高级监考能力（摄像头、人脸、进程锁定）未纳入本轮
