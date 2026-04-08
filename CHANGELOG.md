# Changelog

## [Unreleased]

### 新增
- 新增学生考试中“待复查”持久化标记
- 新增学生答卷回看中心页 `CandidateReviewCenterView`
- 新增学生错题本基础版接口 `GET /api/exam/records/my/wrong-book`
- 新增学生端菜单“答卷回看”
- 新增学生端答卷回看 E2E：`candidate-review-center.spec.ts`
- 新增答题项字段 `review_later_flag`
- 新增复制/粘贴/右键/高风险快捷键拦截
- 新增设备上下文与 IP 留痕
- 新增基础监考策略配置项与工作区策略回传
- 新增登录风险记录页与登录风险日志表
- 新增严格考试态下的单设备限制基础版
- 新增成绩单导出与分析报表导出基础版
- 新增导出流程 E2E：`export-flow.spec.ts`
- 新增账号失败登录次数与锁定状态字段
- 新增认证安全集成测试：`AuthSecurityIntegrationTests.java`
- 新增 MySQL 备份脚本：`scripts/backup-mysql.ps1`
- 新增 MySQL 恢复脚本：`scripts/restore-mysql.ps1`
- 新增登录接口基础压测脚本：`scripts/load-test-login.ps1`
- 新增登录安全告警 runbook 与 MySQL 备份恢复 runbook
- 新增成绩申诉表 `biz_score_appeal`
- 新增阅卷治理集成测试：`GradingGovernanceIntegrationTests.java`
- 新增浏览器回归：`grading-governance.spec.ts`
- 新增考试质量报告接口与 Markdown 导出
- 新增质量报告浏览器回归：`quality-report.spec.ts`
- 新增设备检测浏览器回归：`device-check.spec.ts`
- 新增会话治理集成测试：`SessionGovernanceIntegrationTests.java`
- 新增考试计划保护集成测试：`ExamPlanProtectionIntegrationTests.java`
- 新增试卷保护集成测试：`ExamPaperProtectionIntegrationTests.java`
- 新增配置中心保护集成测试：`ConfigCenterProtectionIntegrationTests.java`

### 变更
- 学生考试工作区保存 / 提交接口同步持久化待复查状态
- 学生成绩详情接口补充富文本题干、材料内容、附件、知识点、章节和待复查标记信息
- 学生成绩列表页新增“答卷回看”跳转
- 监考事件页新增展示 IP、设备指纹和设备摘要
- 教师策略组卷回归用例改为更稳定的 API 轮询断言
- 成绩中心新增“导出成绩单”按钮
- 成绩分析页新增“导出分析报表”按钮
- MySQL 初始化脚本、基线 SQL 与真实 `exam_system` 本地库同步补丁到本轮结构
- MySQL 初始化回归结果中的菜单数量由 21 增加到 23
- 登录接口新增账号锁定、IP 限流与验证码发送限流基础能力
- 登录安全事件达到阈值时，系统会向管理员与机构管理员发送 `SECURITY_ALERT` 站内告警
- MySQL 初始化回归脚本新增安全配置项、登录风险种子和用户安全字段校验
- 真实库 `exam_system` 已补齐 `auth_security` 配置分组和用户安全字段
- 阅卷完成后的答卷不再直接发布，而是先进入待复核状态
- 成绩记录新增复核状态、申诉状态
- 阅卷记录新增评分轮次与评分动作
- 学生成绩详情页新增申诉提交与申诉历史
- 成绩中心新增申诉处理抽屉，支持驳回或转入重判
- 阅卷工作区新增“复核通过并发布 / 退回重判”操作
- 分析页新增“查看质量报告 / 导出质量报告”入口
- 质量报告当前可输出综合质量分、风险提示、建议动作和分考试结论
- 严格考试态下新增进入前设备检测，检测失败时可阻止进入考试工作区
- 设备检测当前会记录通过 / 失败事件，并在监考事件页可查
- 设备检测阈值当前按基础桌面考试环境收口为最小 1200x700，避免误伤正常桌面窗口
- 新登录后旧 token 会立即失效，登出后当前 token 会立即失效
- 考试进行中或已有答卷时，考试计划不再允许被更新或删除
- 考试进行中或已有答卷时，试卷不再允许被更新或删除
- 考试进行中或已有答卷时，高风险配置组不再允许被更新或删除

### 验证
- 后端 `mvn -q test`
- 后端 `mvn -q -DskipTests package`
- 前端 `npm.cmd run build`
- 数据库回归脚本 `scripts/verify-mysql-init.ps1`
- 数据库备份脚本 `scripts/backup-mysql.ps1`
- 数据库恢复脚本 `scripts/restore-mysql.ps1`
- 登录接口基础压测脚本 `scripts/load-test-login.ps1`
- 后端阅卷治理测试 `GradingGovernanceIntegrationTests`
- 后端质量报告接口 smoke
- 后端设备检测策略 smoke
- 后端会话治理与考试计划保护测试
- Playwright 定向回归：`student-exam.spec.ts`、`student-score-flow.spec.ts`、`candidate-review-center.spec.ts`
- Playwright 定向回归：`teacher-exam-workflow.spec.ts`、`student-exam.spec.ts`
- Playwright 定向回归：`login-risk-view.spec.ts`、`export-flow.spec.ts`
- Playwright 定向回归：`grading-governance.spec.ts`
- Playwright 定向回归：`quality-report.spec.ts`
- Playwright 定向回归：`device-check.spec.ts`
- Playwright 全量回归（15 / 15）
