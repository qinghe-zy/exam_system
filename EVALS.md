# 评测与验证说明

## 1. 当前阶段
- 阶段名称：系统能力核查与权限一致性回归阶段
- 评测日期：2026-04-03

## 2. 已完成内容
### 2.1 结构与文档核查
- 已读取根目录正式文档与 `docs/**`
- 已核对后端 controller / service / security / config
- 已核对前端 router / store / views / e2e
- 已核对 MySQL 初始化脚本与本地实库

### 2.2 高优先级修复
- 修复未授权角色可直接进入监考页的前端路由缺口
- 新增权限路由回归用例
- 修复写死时间导致的 E2E 自然过期问题

## 3. 本轮改动文件 / 模块
- `frontend/src/router/index.ts`
- `frontend/tests/e2e/helpers.ts`
- `frontend/tests/e2e/permission-route-guard.spec.ts`
- `frontend/tests/e2e/teacher-exam-workflow.spec.ts`
- `frontend/tests/e2e/student-exam.spec.ts`
- `frontend/tests/e2e/grading-flow.spec.ts`
- 文档与项目记忆文件若干

## 4. 验证结果
### 4.1 数据库与配置验证
- `git remote -v`：`origin` 已指向 `https://github.com/qinghe-zy/exam_system.git`
- `mysql --host=127.0.0.1 --port=3306 --user=root --password=123456 -e "SHOW DATABASES LIKE 'exam_system';"`：通过
- `mysql --host=127.0.0.1 --port=3306 --user=root --password=123456 -D exam_system -e "SHOW TABLES;"`：通过
- MySQL 关键数据量核对：通过

### 4.2 构建与自动化验证
- `backend`: `mvn -q test`：通过
- `frontend`: `npm.cmd run build`：通过
- `frontend`: `npx.cmd playwright test`：通过（6 / 6）

### 4.3 Playwright 真实流程验证
1. 教师端主流程
- `teacher-paper-plan.spec.ts`：通过
- `teacher-exam-workflow.spec.ts`：通过
- 覆盖内容：
  - 登录
  - 新建策略组卷
  - 发布考试
  - 学生进入考试
  - 触发监考事件
  - 后台查询事件

2. 学生端主流程
- `student-exam.spec.ts`：通过
- 覆盖内容：
  - 登录
  - 查看待考列表
  - 进入考试
  - 倒计时
  - 答题卡
  - 手动保存
  - 提交确认
  - 提交试卷

3. 阅卷与成绩流程
- `grading-flow.spec.ts`：通过
- 覆盖内容：
  - 阅卷老师登录
  - 打开阅卷任务
  - 主观题评分
  - 提交评分

4. 权限流程
- `permission-route-guard.spec.ts`：通过
- 覆盖内容：
  - 学生直达未授权监考页
  - 被前端路由守卫自动拦回首页看板
  - 未触发监考接口 403 请求

5. AI 题库辅助流程
- `question-ai.spec.ts`：通过
- 覆盖内容：
  - 题库页 AI 入口可见
  - 未配置密钥时返回明确中文提示

### 4.4 手工与脚本补充核查
- 使用 Playwright 脚本复现并确认：
  - 修复前学生访问 `/exam/proctor` 时页面骨架可见且接口返回 403
  - 修复后学生访问同一路由时直接被拦回可访问入口

## 5. 剩余风险
1. 当前 AI 真实成功调用仍受本地 `AI_API_KEY` 缺失限制。
2. 浏览器验证当前以 Edge 为主，尚未形成 Chrome / Firefox / Safari 矩阵。
3. 当前未做并发压测与高峰考试保护验证。

## 6. 下一步建议
1. 在下一轮补充学生侧成绩查看与错题闭环后，再新增对应 Playwright 用例。
2. 若进入安全增强阶段，优先补登录风控与限流回归。
