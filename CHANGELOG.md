# Changelog

## [Unreleased]

### 新增
- 新增学生成绩列表与成绩详情页
- 新增成绩详情 VO 与学生专用成绩接口
- 新增运行时健康检查接口
- 新增数据库初始化回归脚本 `scripts/verify-mysql-init.ps1`
- 新增学生成绩流程 E2E：`student-score-flow.spec.ts`

### 变更
- 收紧题库、试卷、成绩、角色、菜单和公告管理接口权限
- 消息中心支持跳转到成绩详情和我的考试
- 初始化脚本与运行时种子数据新增“我的成绩”菜单及已发布成绩样例
- 更新 README、API 文档、测试文档、交接文档和产品核查文档

### 验证
- 后端 `mvn -q test`
- 后端 `mvn -q -DskipTests package`
- 前端 `npm.cmd run build`
- 数据库回归脚本 `scripts/verify-mysql-init.ps1`
- 运行态 HTTP smoke
- Playwright 全量回归（7 / 7）
