# 开发记录

## 1. 当前阶段
- 阶段名称：账号与权限增强、题库与题型增强阶段
- 当前状态：已在上一轮基础上继续补齐注册/找回密码、验证码 mock 通道、按钮级权限基础版、题型扩展、富文本/附件基础支持和知识点自动组题基础版

## 2. 已完成内容
### 2.1 核查与文档
- 已完成 12 大模块能力核查并输出正式中文文档：
  - `docs/product/系统功能核查矩阵.md`
  - `docs/product/系统能力差距分析.md`
- 已将根目录交付文档、测试文档、产品文档与数据库 runbook 同步到本轮真实状态。

### 2.2 功能补全
1. 权限与一致性
- 收紧了题库、试卷、成绩、角色、菜单等管理端接口权限。
- 保留学生可访问的消息中心、考试中心与学生专用成绩接口。
- 前端路由守卫继续保持菜单/路由/接口一致。
- 前端新增按钮级权限基础版，关键页面按钮按当前账号权限目录显示。

2. 账号生命周期增强
- 登录页新增：
  - 登录
  - 学生注册
  - 找回密码
- 新增注册可选班级/部门接口。
- 新增验证码发送接口，支持邮箱/短信 mock 通道。
- 新增学生注册接口与找回密码接口。
- 新增验证码表 `sys_verification_code`。

3. 学生考后闭环
- 新增学生“我的成绩”页面：`/candidate/scores`
- 新增学生成绩详情抽屉，展示：
  - 本场成绩
  - 逐题得分
  - 我的答案
  - 参考答案
  - 解析说明
  - 阅卷备注
- 新增后端接口：
  - `GET /api/exam/records/my`
  - `GET /api/exam/records/my/{id}`

4. 通知协同
- 消息中心新增“查看详情”跳转：
  - `SCORE_RECORD` 跳成绩详情
  - `EXAM_PLAN` 跳我的考试

5. 题库与题型增强
- 题库新增：
  - 填空题
  - 论述题
  - 材料题基础版
  - 题干富文本 HTML
  - 材料内容
  - 附件 JSON
  - 使用次数统计
  - 按知识点自动组题基础版
- 考生端与阅卷端已补新题型基础渲染与评分适配：
  - 填空题支持基础自动判分
  - 材料题支持材料正文渲染

6. 运维基础能力
- 新增运行时健康检查接口：
  - `GET /api/system/runtime/health`
- 新增数据库初始化回归脚本：
  - `scripts/verify-mysql-init.ps1`

### 2.3 测试增强
- 新增 E2E：
  - `frontend/tests/e2e/permission-route-guard.spec.ts`
  - `frontend/tests/e2e/student-score-flow.spec.ts`
- 新增 E2E 辅助：
  - `frontend/tests/e2e/helpers.ts`
- 扩展后端回归测试：
  - 学生不能直接调管理接口
  - 学生可以调自己的成绩接口
  - 管理员可以调运行时健康检查接口

## 3. 本轮改动文件 / 模块
### 3.1 后端
- `backend/src/main/java/com/projectexample/examsystem/controller/AuthController.java`
- `backend/src/main/java/com/projectexample/examsystem/controller/QuestionBankController.java`
- `backend/src/main/java/com/projectexample/examsystem/controller/ExamPaperController.java`
- `backend/src/main/java/com/projectexample/examsystem/controller/ExamRecordController.java`
- `backend/src/main/java/com/projectexample/examsystem/controller/SysRoleController.java`
- `backend/src/main/java/com/projectexample/examsystem/controller/SysMenuController.java`
- `backend/src/main/java/com/projectexample/examsystem/controller/NoticeController.java`
- `backend/src/main/java/com/projectexample/examsystem/controller/RuntimeController.java`
- `backend/src/main/java/com/projectexample/examsystem/entity/VerificationCode.java`
- `backend/src/main/java/com/projectexample/examsystem/mapper/VerificationCodeMapper.java`
- `backend/src/main/java/com/projectexample/examsystem/security/RolePermissionCatalog.java`
- `backend/src/main/java/com/projectexample/examsystem/dto/*`（注册、找回密码、知识点自动组题 DTO）
- `backend/src/main/java/com/projectexample/examsystem/service/ExamRecordService.java`
- `backend/src/main/java/com/projectexample/examsystem/service/AuthService.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/AuthServiceImpl.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/QuestionBankServiceImpl.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/ExamRecordServiceImpl.java`
- `backend/src/main/java/com/projectexample/examsystem/vo/ExamRecordVO.java`
- `backend/src/main/java/com/projectexample/examsystem/vo/CandidateScoreDetailVO.java`
- `backend/src/main/java/com/projectexample/examsystem/vo/CandidateScoreItemVO.java`
- `backend/src/main/java/com/projectexample/examsystem/vo/RuntimeHealthVO.java`

### 3.2 前端
- `frontend/src/views/login/LoginView.vue`
- `frontend/src/router/index.ts`
- `frontend/src/hooks/usePermission.ts`
- `frontend/src/api/exam.ts`
- `frontend/src/api/auth.ts`
- `frontend/src/types/exam.ts`
- `frontend/src/views/exam/CandidateScoreView.vue`
- `frontend/src/views/exam/QuestionBankView.vue`
- `frontend/src/views/exam/CandidateExamView.vue`
- `frontend/src/views/notices/MessageCenterView.vue`
- `frontend/src/views/notices/NoticeView.vue`

### 3.3 测试与脚本
- `frontend/tests/e2e/auth-account-flow.spec.ts`
- `frontend/tests/e2e/student-score-flow.spec.ts`
- `frontend/tests/e2e/question-type-enhancement.spec.ts`
- `frontend/tests/e2e/permission-route-guard.spec.ts`
- `frontend/tests/e2e/helpers.ts`
- `backend/src/test/java/com/projectexample/examsystem/ApiSmokeIntegrationTests.java`
- `backend/src/test/java/com/projectexample/examsystem/AuthLifecycleIntegrationTests.java`
- `backend/src/test/java/com/projectexample/examsystem/PermissionMatrixIntegrationTests.java`
- `backend/src/test/java/com/projectexample/examsystem/QuestionTypeEnhancementIntegrationTests.java`
- `scripts/verify-mysql-init.ps1`

### 3.4 数据与文档
- `sql/mysql/init.sql`
- `backend/src/main/resources/data.sql`
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/application.yml`
- `README.md`
- `PRD.md`
- `DATA_MODEL.md`
- `SECURITY.md`
- `docs/api/core-flows.md`
- `docs/testing/smoke-test.md`
- `docs/runbooks/mysql-init.md`
- `docs/data/测试数据说明.md`
- 其余根目录交付文档与项目记忆文件

## 4. 验证结果
### 4.1 构建与测试
- 后端：`mvn -q test` 通过
- 后端：`mvn -q -DskipTests package` 通过
- 前端：`npm.cmd run build` 通过
- 前端：`npx.cmd playwright test` 通过（9 / 9）

### 4.2 数据库与启动验证
- `scripts/verify-mysql-init.ps1`：通过
- 临时库回归结果：
  - 菜单 21
  - 用户 74
  - 题目 320
  - 试卷 8
  - 考试 6
  - 成绩记录 12
- MySQL 模式后端启动：通过

### 4.3 真实 HTTP smoke
- 管理员运行时健康检查：通过
- 注册选项、验证码发送、注册、找回密码：通过
- 教师访问题库接口：通过
- 教师按知识点自动组题接口：通过
- 学生访问自己的成绩列表：通过
- 学生访问自己的成绩详情：通过
- 学生访问题库管理接口：403，符合预期

### 4.4 浏览器与主流程验证
- 教师端：组卷、发布、策略卷、考试链路通过
- 教师端：填空题创建、知识点自动组题通过
- 学生端：进入考试、保存、提交、倒计时通过
- 学生端：注册、找回密码、新密码登录通过
- 阅卷端：主观题评分通过
- 权限端：学生直达未授权监考页被拦回通过
- 通知/成绩端：消息中心进入成绩详情通过
- AI 题库辅助缺配置提示链路通过

## 5. 剩余风险
1. 复杂题型、多媒体题和公式/代码编辑器仍未实现。
2. 学生错题本、个人答题轨迹、申诉复核未实现。
3. 防作弊仍是浏览器侧基础留痕，不是完整监考平台。
4. 登录风控、限流、告警、性能监控、灾备和高并发保护仍不足。
5. AI 真实成功调用仍受 `AI_API_KEY` 缺失限制。

## 6. 下一步建议
1. 优先继续补学生错题闭环与个人答卷回看。
2. 优先扩展题型和富媒体支持。
3. 继续补登录风控、健康巡检和备份恢复等运维安全基线。
