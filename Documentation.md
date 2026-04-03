# 开发记录

## 1. 当前阶段
- 阶段名称：系统能力全量核查、权限一致性修复与浏览器回归验证阶段
- 当前状态：已完成“核查 -> 补缺口 -> Playwright 回归 -> 文档回写”闭环，仓库进入可提交收口状态

## 2. 已完成内容
### 2.1 完成全量系统能力核查
- 已读取并核对根目录正式文档、`docs/**`、数据库说明和模块 README。
- 已核对后端控制器、服务、权限注解、组织范围过滤逻辑。
- 已核对前端路由、菜单、考试页面、题库页面、试卷与考试发布页面。
- 已核对 `sql/mysql/init.sql`、`schema.sql`、`data.sql` 与本地 MySQL `exam_system` 实库。
- 已新增正式中文文档：
  - `docs/product/系统功能核查矩阵.md`
  - `docs/product/系统能力差距分析.md`

### 2.2 基于核查结果补齐高优先级缺口
- 发现并修复高优先级权限一致性问题：
  - 修复前：学生可直接访问 `/exam/proctor`，页面骨架能打开，但接口返回 403，形成“页面能进、数据加载失败、出现 Access Denied”的半残状态。
  - 修复后：前端在进入受保护页面前，会根据当前菜单树与角色可见页面进行路由拦截，并自动回退到当前账号可访问入口。
- 修复位置：
  - `frontend/src/router/index.ts`

### 2.3 补强浏览器回归用例
- 新增权限回归用例：
  - `frontend/tests/e2e/permission-route-guard.spec.ts`
- 修复既有教师链路用例的脆弱点：
  1. 不再误点考试列表第一行，而是精确定位本次创建的考试。
  2. 将写死的绝对时间改为基于当前时间动态生成，避免测试在当天晚些时候自然失效。
- 新增共享测试辅助：
  - `frontend/tests/e2e/helpers.ts`
- 同步更新以下 E2E：
  - `teacher-exam-workflow.spec.ts`
  - `student-exam.spec.ts`
  - `grading-flow.spec.ts`

## 3. 本轮改动文件 / 模块
### 3.1 代码
- `frontend/src/router/index.ts`
- `frontend/tests/e2e/helpers.ts`
- `frontend/tests/e2e/permission-route-guard.spec.ts`
- `frontend/tests/e2e/teacher-exam-workflow.spec.ts`
- `frontend/tests/e2e/student-exam.spec.ts`
- `frontend/tests/e2e/grading-flow.spec.ts`

### 3.2 文档
- `docs/product/系统功能核查矩阵.md`
- `docs/product/系统能力差距分析.md`
- `Documentation.md`
- `EVALS.md`
- `HANDOFF.md`
- `PLAN.md`
- `DECISIONS.md`
- `CHANGELOG.md`
- `docs/testing/README.md`
- `docs/product/README.md`
- `database/README.md`
- `frontend/README.md`
- `task_plan.md`
- `findings.md`
- `progress.md`

## 4. 验证结果
### 4.1 文档与数据库核查
- 已确认远端仓库 `origin` 为：`https://github.com/qinghe-zy/exam_system.git`
- 已确认本地 MySQL `exam_system` 存在
- 已确认本地 MySQL 关键表存在：
  - `biz_question_bank`
  - `biz_exam_paper`
  - `biz_exam_plan`
  - `biz_exam_candidate`
  - `biz_answer_sheet`
  - `biz_answer_item`
  - `biz_grading_record`
  - `biz_score_record`
  - `biz_anti_cheat_event`
  - `biz_audit_log`
  - `biz_in_app_message`
- 已核对关键数据量：
  - 组织 12
  - 角色 6
  - 用户 74
  - 菜单 20
  - 题目 320
  - 当前数据库中试卷、考试、答卷、成绩和监考事件数据均已存在，并能被页面与接口读取

### 4.2 构建与自动化验证
- 后端：`mvn -q test` 通过
- 前端：`npm.cmd run build` 通过
- 前端：`npx.cmd playwright test` 通过（6 / 6）

### 4.3 浏览器与主流程验证
- 教师端：
  - 题库页 AI 草稿入口：通过
  - 试卷管理 -> 新建试卷：通过
  - 随机组卷 / 策略组卷：通过
  - 考试发布：通过
- 学生端：
  - 待考列表进入考试：通过
  - 倒计时展示：通过
  - 保存答案：通过
  - 提交确认：通过
  - 自动交卷链路：通过
- 阅卷端：
  - 阅卷任务列表：通过
  - 主观题评分：通过
  - 成绩发布提醒：通过
- 防作弊：
  - 切屏 / 失焦 / 全屏退出事件链路：通过
  - 事件落库并在监考页可查：通过
- 权限：
  - 学生直达未授权监考页会被路由守卫拦回：通过
  - 未再出现本轮核查前的“页面骨架可见、接口 403”半残状态

## 5. 剩余风险
1. 当前题型仍以单选、多选、判断、简答为主，复杂题型和多媒体题尚未实现。
2. 防作弊仍是浏览器侧基础留痕，不是完整智能监考系统。
3. 学生侧成绩查看、错题本、考后解析仍未补齐。
4. 安全与运维能力仍偏弱，缺少登录风控、限流、健康检查、告警、备份恢复与并发压测。
5. AI 当前只接入题库辅助，且当前环境未配置真实 `AI_API_KEY`，因此只验证到“入口、后端调用链和缺配置中文反馈”。

## 6. 下一步建议
1. 继续优先补齐题型扩展、知识点组卷和学生考后闭环。
2. 对服务端数据权限做更细的行级策略收口，并增加按钮级授权校验。
3. 增加登录风控、限流、健康检查和备份恢复 runbook，提升运维交付能力。
4. 若进入下一轮，应优先在学生侧开放成绩查看和错题分析，以增强验收观感与学习闭环。
