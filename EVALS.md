# 评测与验证说明

## 1. 当前阶段
- 阶段名称：会话治理与考试计划保护增强阶段
- 评测日期：2026-04-08

## 2. 已完成内容
### 2.1 功能补全
- 登录态会话治理基础版
- 新登录使旧 token 失效
- 登出使当前 token 失效
- 考试进行中计划更新 / 删除保护
- 考试进行中试卷更新 / 删除保护
- 考试进行中高风险配置更新 / 删除保护

### 2.2 测试增强
- 新增 `SessionGovernanceIntegrationTests`
- 新增 `ExamPlanProtectionIntegrationTests`
- 新增 `ExamPaperProtectionIntegrationTests`
- 新增 `ConfigCenterProtectionIntegrationTests`
- 保持后端全量测试通过
- Playwright 全量回归改为串行执行，以匹配单账号单会话治理规则

## 3. 本轮改动文件 / 模块
- `backend/src/main/java/com/projectexample/examsystem/security/JwtTokenProvider.java`
- `backend/src/main/java/com/projectexample/examsystem/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/projectexample/examsystem/security/UserPrincipal.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/AuthServiceImpl.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/ExamPlanServiceImpl.java`
- `backend/src/main/java/com/projectexample/examsystem/entity/SysUser.java`
- `backend/src/test/java/com/projectexample/examsystem/SessionGovernanceIntegrationTests.java`
- `backend/src/test/java/com/projectexample/examsystem/ExamPlanProtectionIntegrationTests.java`
- `backend/src/test/java/com/projectexample/examsystem/ExamPaperProtectionIntegrationTests.java`
- `backend/src/test/java/com/projectexample/examsystem/ConfigCenterProtectionIntegrationTests.java`
- `frontend/playwright.config.ts`
- `backend/src/main/resources/schema.sql`
- `sql/schema-baseline.sql`
- `sql/mysql/init.sql`

## 4. 验证结果
### 4.1 构建验证
- 后端 `mvn -q test`：通过
- 前端 `npm.cmd run build`：通过
- Playwright 全量回归：15 / 15 通过（串行）

### 4.2 数据库回归
- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-mysql-init.ps1`：通过
- 回归校验结果：
  - `sys_menu`：23
  - `sys_user`：74
  - `sys_config_item`：28
  - `biz_question_bank`：320
  - `biz_exam_paper`：8
  - `biz_exam_plan`：6
  - `biz_score_record`：12
  - `biz_score_appeal`：0
  - `biz_login_risk_log`：4
  - `sys_user` 安全字段：4
  - `biz_score_record` 治理字段：2

### 4.3 真实库校验
- 本地 MySQL `exam_system` 已补齐：
  - `sys_user.session_version`
  - `anti_cheat` 分组配置项：14

### 4.4 会话治理验证
- `SessionGovernanceIntegrationTests`：通过
- 关键断言覆盖：
  - 同一账号重新登录后，旧 token 立即失效
  - 主动登出后，当前 token 立即失效

### 4.5 防误操作验证
- `ExamPlanProtectionIntegrationTests`：通过
- 关键断言覆盖：
  - 考试已开始时，不允许更新考试计划
  - 考试已开始时，不允许删除考试计划

### 4.6 试卷与配置只读保护验证
- `ExamPaperProtectionIntegrationTests`：通过
- `ConfigCenterProtectionIntegrationTests`：通过
- 关键断言覆盖：
  - 当前试卷已被进行中的考试使用时，不允许更新或删除试卷
  - 当前存在已开始考试时，不允许更新高风险配置组

## 5. 剩余风险
1. 浏览器矩阵仍以 Edge 为主，尚未覆盖更多浏览器。
2. 当前更强考试态控制仍未覆盖更多后台模块，例如考试期配置只读保护和更细多窗口识别。
3. 当前设备检测仍是基础版，尚未覆盖摄像头、麦克风、活体或更强设备指纹校验。
4. 当前质量报告仍缺班级/年级/部门对比与趋势分析。

## 6. 下一步建议
1. 继续补更强考试态控制，如更多后台模块的考试期只读保护和更细多窗口识别。
2. 继续补通知模板与外部通知通道。
3. 继续补班级/年级/部门对比与趋势分析。
