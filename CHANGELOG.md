# Changelog

## [Unreleased]

### 新增
- 新增正式中文核查文档：
  - `docs/product/系统功能核查矩阵.md`
  - `docs/product/系统能力差距分析.md`
- 新增 Playwright 权限回归用例：`permission-route-guard.spec.ts`
- 新增 E2E 时间辅助：`frontend/tests/e2e/helpers.ts`

### 变更
- 修复前端路由权限一致性问题，避免未授权角色直接访问监考页后出现“页面可见、接口 403”的半残状态
- 将教师、学生、阅卷相关 E2E 中写死的考试时间改为基于当前时间动态生成
- 修复教师策略组卷工作流回归用例中误点考试列表首行的问题
- 回写根目录与产品、测试、交接、决策文档，统一本轮核查与修复结论

### 验证
- MySQL 实库核查
- 后端 `mvn -q test`
- 前端 `npm.cmd run build`
- 前端 `npx.cmd playwright test`
- 学生未授权路由拦截回归
- 教师组卷/发布/学生进入/监考事件链路回归
