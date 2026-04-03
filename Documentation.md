# 开发记录

## 一、当前阶段
- 阶段名称：试卷组卷链路、考试时间语义与基础防作弊闭环修复阶段
- 当前状态：已完成代码修复、MySQL 重建验证、浏览器级回归与文档同步，可进入验收

## 二、已完成内容
### 1. 新建考试 / 新建试卷 / 组卷链路重构
- 将教师端“新建试卷”改造成一体化试卷工作台，在同一弹窗内完成：
  - 试卷编码、名称、学科、组卷方式、时长、及格线、版本、备注、卷面说明、发布状态
  - 题库筛题：按学科、题型、难度、关键字过滤
  - 卷面组成：加入、移除、上移、下移、调整分值、设置必答/选答
  - 组卷蓝图：题型分布、难度分布、每题型默认分值、乱序开关
- 新增“快速随机组卷 / 快速策略组卷”入口，策略组卷现在会真实校验：
  - 当前学科题库数量是否足够
  - 题型分布合计与难度分布合计是否一致
  - 某一题型或某一难度是否存在题目短缺
  - 题型 × 难度组合是否能实际分配成功
- 试卷保存不再依赖前端“猜总分”：
  - 后端会按已选题目分值重新计算 `totalScore`
  - 校验 `passScore <= totalScore`
  - 拒绝重复题目、跨组织混卷
- 试卷数据模型补齐：
  - `paper_version`
  - `remark_text`
  - `shuffle_enabled`
  - `question_type_config_json`
  - `difficulty_config_json`

### 2. 新建考试交互重构
- 将“新建考试计划”改为“新建考试”语义，页面重点从“字段填写”改为“规则说明”。
- 新版考试弹窗明确区分：
  - 试卷摘要：题量、总分、时长、卷面说明
  - 进入窗口：开始时间、结束时间、最晚进入规则
  - 实际作答规则：学生进入后按时长倒计时，而不是直接按结束时间倒计时
  - 自动交卷条件：剩余时间归零后是否自动交卷
  - 考生名单：多选并展示已选考生摘要
- 后端考试计划保存新增校验：
  - 结束时间必须晚于开始时间
  - 及格线不能高于试卷总分
  - 非管理员创建考试时，试卷与考生必须在本组织可访问范围内

### 3. 学生端考试时间语义修复
- 纠正了原有“学生倒计时直接等于 `endTime - now`”的错误逻辑。
- 当前规则已统一为：
  - 老师设置的 `startTime / endTime` 表示学生允许进入考试的时间窗口
  - 若设置 `lateEntryMinutes > 0`，则最晚进入时间 = `min(endTime, startTime + lateEntryMinutes)`
  - 学生实际作答截止时间 = `min(startedAt + durationMinutes, endTime)`
  - 学生页显示的倒计时 = `answerDeadlineAt - now`
- 本轮已通过真实验证：
  - 新建测试考试 `AUTO-KS-*` 后，学生进入页面显示 `01:30:00`
  - 页面同时展示“允许进入窗口”和“自动交卷规则”

### 4. 学生端考试界面沉浸式改造
- 去掉右侧抽屉式工作区，改为固定全屏考试态覆盖层。
- 重新组织页面层级：
  - 顶部固定考试抬头：考试名、试卷版本、作答倒计时、进入窗口、自动交卷规则
  - 中部左侧主答题区：题目卡片、清晰题序、分值标签
  - 中部右侧固定答题卡：答题进度、当前题目聚焦、考试状态摘要
  - 异常与保存信息上收至顶部工具栏，减少对题目区干扰
- 新考试态仍保留完整功能：
  - 保存答案
  - 自动保存
  - 进入全屏
  - 提交试卷
  - 答题卡跳转

### 5. 基础防作弊闭环补齐
- 学生端现在真实绑定以下检测逻辑：
  - `visibilitychange`：页面切换 / 标签页切换
  - `blur`：窗口失焦
  - `fullscreenchange`：退出全屏
- 检测触发后不再只写一句提示，而是会：
  1. 先触发一次自动保存
  2. 再把事件写入后端
  3. 同步记录 `leaveCount`、`triggeredAutoSave`、`saveVersion`
- 数据库 `biz_anti_cheat_event` 已新增字段：
  - `leave_count`
  - `triggered_auto_save`
  - `save_version`
- 后台监考页已升级为可查看：
  - 考试名称
  - 考生姓名
  - 事件类型
  - 严重级别
  - 累计次数
  - 是否联动自动保存
  - 答卷版本
- 已通过真实链路验证：
  - 学生进入自动化回归考试后，触发 `TAB_SWITCH` / `WINDOW_BLUR` / `FULLSCREEN_EXIT`
  - 事件真实写入 MySQL
  - 答卷 `save_version` 从 1 增长到 3
  - 管理员端 `/exam/proctor` 页面可查询到事件

### 6. AI 模块核查结论
- 当前项目不是“完全没有 AI 代码”，而是“仅做了接入预留，没有真实业务接入”。
- 已核实位置如下：
  - 后端配置：`backend/src/main/resources/application.yml`
  - 后端适配层：`backend/src/main/java/com/projectexample/examsystem/infra/ai/AiGatewayProperties.java`
  - 后端客户端占位：`backend/src/main/java/com/projectexample/examsystem/infra/ai/AiGatewayClient.java`
  - 示例环境变量：`.env.example`
- 当前没有发现：
  - 前端 AI 页面入口
  - 后端 AI controller
  - 后端 AI service 业务编排
  - 实际模型调用代码
- AI Key 位置说明：
  - `.env.example`：`AI_API_BASE_URL` / `AI_API_KEY` / `AI_MODEL`
  - `application.yml`：`app.ai.api-base-url` / `app.ai.api-key` / `app.ai.model`
  - 当前仓库未发现 `.env.local`、`application-local.yml`、前端 env 中的真实 AI key 配置
  - 当前不暴露任何真实 key

### 7. 外部参考吸收结论
- 本轮仅做了克制检索，并将结论吸收到当前实现，不直接照抄外部产品。
- 高价值结论：
  - 定时考试应把“可进入窗口”和“答题时长”分开表达，否则学生会混淆自己还能不能进入与还能答多久。
  - 自动交卷或超时提交规则必须在页面上明确说明，否则学生会误把“宽限提交”理解成“额外作答时间”。
  - 题目分页/自动保存对降低网络抖动带来的损失非常重要，异常行为触发自动保存也属于同一原则。
- 参考来源：
  - [Moodle 定时考试与宽限提交说明](https://ltc.uow.edu.au/hub/article/quiz-timed-tasks)

## 三、本轮改动文件 / 模块
### 1. 后端
- `backend/src/main/java/com/projectexample/examsystem/common/PaperRuleConfigItem.java`
- `backend/src/main/java/com/projectexample/examsystem/entity/ExamPaper.java`
- `backend/src/main/java/com/projectexample/examsystem/entity/AntiCheatEvent.java`
- `backend/src/main/java/com/projectexample/examsystem/dto/ExamPaperSaveRequest.java`
- `backend/src/main/java/com/projectexample/examsystem/dto/PaperQuestionItemRequest.java`
- `backend/src/main/java/com/projectexample/examsystem/dto/CandidateEventReportRequest.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/ExamPaperServiceImpl.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/ExamPlanServiceImpl.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/CandidateExamServiceImpl.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/AntiCheatServiceImpl.java`
- `backend/src/main/java/com/projectexample/examsystem/vo/ExamPaperVO.java`
- `backend/src/main/java/com/projectexample/examsystem/vo/PaperQuestionItemVO.java`
- `backend/src/main/java/com/projectexample/examsystem/vo/CandidateExamVO.java`
- `backend/src/main/java/com/projectexample/examsystem/vo/CandidateExamWorkspaceVO.java`
- `backend/src/main/java/com/projectexample/examsystem/vo/AntiCheatEventVO.java`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/data.sql`
- `sql/mysql/init.sql`

### 2. 前端
- `frontend/src/views/exam/ExamPaperView.vue`
- `frontend/src/views/exam/ExamPlanView.vue`
- `frontend/src/views/exam/CandidateExamView.vue`
- `frontend/src/views/exam/ProctorView.vue`
- `frontend/src/types/exam.ts`
- `frontend/src/api/exam.ts`
- `frontend/src/constants/exam.ts`
- `frontend/src/utils/labels.ts`

### 3. 测试
- `frontend/tests/e2e/teacher-paper-plan.spec.ts`
- `frontend/tests/e2e/student-exam.spec.ts`
- `frontend/tests/e2e/teacher-exam-workflow.spec.ts`

## 四、验证结果
### 1. 构建 / 测试
- 后端 `mvn -q test`：通过
- 后端 `mvn -q -DskipTests package`：通过
- 前端 `npm.cmd run build`：通过
- 前端 `npm.cmd run test:e2e`：4 条 E2E 全部通过

### 2. MySQL 重建与结构验证
- 已删除并重建 `exam_system`
- 已重新导入 `sql/mysql/init.sql`
- 已验证新增字段存在：
  - `biz_exam_paper.paper_version`
  - `biz_exam_paper.remark_text`
  - `biz_exam_paper.shuffle_enabled`
  - `biz_exam_paper.question_type_config_json`
  - `biz_exam_paper.difficulty_config_json`
  - `biz_anti_cheat_event.leave_count`
  - `biz_anti_cheat_event.triggered_auto_save`
  - `biz_anti_cheat_event.save_version`

### 3. 真实业务回归
- 教师快速策略组卷并保存试卷：成功，新增 `AUTO-*` 试卷已落库
- 教师发布测试考试：成功，新增 `AUTO-KS-*` 考试计划已落库
- 学生进入考试：
  - 允许进入窗口显示正确
  - 实际作答倒计时显示正确
  - 答卷状态由 `NOT_STARTED` 变为 `IN_PROGRESS`
- 防作弊事件：
  - `TAB_SWITCH` / `WINDOW_BLUR` / `FULLSCREEN_EXIT` 已真实写入 MySQL
  - 事件已记录 `triggered_auto_save` 与 `save_version`
  - 管理员页 `/exam/proctor` 可见事件数据

## 五、剩余风险
- 当前基础防作弊仍是浏览器侧检测与留痕，不具备摄像头、人脸、设备锁定、进程级封禁能力。
- `element-plus` vendor chunk 仍有体积告警，但不影响当前功能正确性。
- 本轮自动化验证中的 `visibilitychange` / `fullscreenchange` 由浏览器事件驱动代码路径触发，用于确认检测链路与落库链路可用；真实人工演示时建议再做一次手动切屏与退出全屏验证。
- 当前仓库已有用户自己的未提交改动：`frontend/tests/e2e/grading-flow.spec.ts`，本轮未覆盖该文件内容。

## 六、下一步建议
1. 继续补充更强的数据权限约束，尤其是教师、监考员、机构管理员的组织边界。
2. 若进入下一阶段，优先扩展防作弊到设备锁定、摄像头/麦克风、更多行为统计。
3. 继续完善考试发布后的消息通知与批量操作能力。
