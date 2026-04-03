# 评测与验证说明

## 1. 当前阶段
- 阶段名称：功能补全与可验收测试增强阶段
- 评测日期：2026-04-03

## 2. 已完成内容
### 2.1 功能补全
- 学生注册与找回密码基础版
- 邮箱 / 短信验证码 mock 通道
- 按钮级权限基础版
- 新题型（填空题、论述题、材料题）
- 富文本 HTML、材料内容与附件 JSON 基础支持
- 题目使用次数统计基础版
- 按知识点自动组题基础版
- 学生已发布成绩列表与成绩详情页
- 消息中心跳转成绩详情
- 管理端接口权限收紧
- 运行时健康检查接口
- MySQL 初始化回归脚本

### 2.2 测试增强
- 新增学生成绩流程 E2E
- 新增权限矩阵断言
- 新增数据库临时库初始化回归脚本
- 新增运行态 HTTP smoke 校验

## 3. 本轮改动文件 / 模块
- `frontend/src/views/exam/CandidateScoreView.vue`
- `frontend/src/views/notices/MessageCenterView.vue`
- `frontend/src/views/notices/NoticeView.vue`
- `frontend/src/router/index.ts`
- `frontend/src/api/exam.ts`
- `frontend/src/types/exam.ts`
- `frontend/tests/e2e/student-score-flow.spec.ts`
- `frontend/tests/e2e/*`
- `backend/src/main/java/com/projectexample/examsystem/controller/*`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/ExamRecordServiceImpl.java`
- `scripts/verify-mysql-init.ps1`

## 4. 验证结果
### 4.1 构建验证
- 后端 `mvn -q test`：通过
- 后端 `mvn -q -DskipTests package`：通过
- 前端 `npm.cmd run build`：通过

### 4.2 数据库回归
- `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-mysql-init.ps1`：通过
- 回归校验结果：
  - `sys_menu`：21
  - `sys_user`：74
  - `biz_question_bank`：320
  - `biz_exam_paper`：8
  - `biz_exam_plan`：6
  - `biz_score_record`：12

### 4.3 后端运行态 HTTP smoke
- 注册选项：通过
- 注册验证码发送：通过
- 注册：通过
- 找回密码验证码发送：通过
- 找回密码：通过
- 管理员登录：通过
- `GET /api/system/runtime/health`：通过
- 学生登录：通过
- 教师 `POST /api/exam/questions/auto-group/knowledge-points`：通过
- `GET /api/exam/records/my`：通过
- `GET /api/exam/records/my/{id}`：通过
- 教师 `GET /api/exam/questions`：通过
- 学生 `GET /api/exam/questions`：403，符合预期

### 4.4 Playwright 浏览器回归
- `auth-account-flow.spec.ts`：通过
- `teacher-paper-plan.spec.ts`：通过
- `teacher-exam-workflow.spec.ts`：通过
- `student-exam.spec.ts`：通过
- `grading-flow.spec.ts`：通过
- `question-ai.spec.ts`：通过
- `question-type-enhancement.spec.ts`：通过
- `permission-route-guard.spec.ts`：通过
- `student-score-flow.spec.ts`：通过

### 4.5 浏览器验证覆盖点
1. 管理员 / 教师
- 题库与建卷入口
- 策略组卷
- 考试发布

2. 学生
- 注册、找回密码、新密码登录
- 待考列表
- 进入考试
- 保存与提交
- 成绩查看
- 消息跳转详情

3. 阅卷老师
- 阅卷任务
- 主观题评分

4. 权限
- 未授权路由拦截
- 管理接口 403 收口
- 关键按钮按权限目录显示

## 5. 剩余风险
1. 浏览器矩阵仍以 Edge 为主
2. 没有并发压测与弱网专项测试
3. AI 真实成功调用仍未完成

## 6. 下一步建议
1. 补学生错题本与个人答卷回看
2. 补题型与富媒体支持
3. 补登录风控和高并发保护
