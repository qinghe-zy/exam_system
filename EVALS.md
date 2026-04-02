# 评测与验证说明

## 一、文档目的
本文档记录当前项目的构建、数据库回归、接口 smoke、浏览器级 E2E 与中文测试数据验收结果。

## 二、验证矩阵
### 1. 构建验证
- 后端 `mvn -q test`：通过
- 后端 `mvn -q -DskipTests package`：通过
- 前端 `npm.cmd run build`：通过

### 2. 数据库回归验证
已执行：
1. 删除并重建 `exam_system`
2. 导入 `sql/mysql/init.sql`
3. 验证中文测试数据规模
4. 验证关键账号登录
5. 验证 MySQL 模式关键接口读取数据库

当前结果：
- 学科字典：8
- 题目：320
- 教师：8
- 学生：60
- 组织：12
- 试卷：8
- 考试计划：6

### 3. 关键账号登录 smoke
- 管理员：`900001 / 123456` 通过
- 教师：`800001 / 123456` 通过
- 学生：`20260001 / 123456` 通过

### 4. 接口 smoke
已验证：
- `/api/auth/login`
- `/api/system/organizations`
- `/api/system/users`
- `/api/system/config-center/configs`
- `/api/messages/my`
- `/api/exam/questions/export`
- `/api/exam/candidate/exams/1?examPassword=YW2026`

### 5. 浏览器级 E2E
已通过场景：
1. 学生登录 -> 进入考试 -> 作答 -> 提交
2. 阅卷老师登录 -> 打开阅卷任务 -> 评分 -> 提交

## 三、前端优化结果
- 已完成路由懒加载
- 已完成 Vite 手工拆包
- 主入口体积已缩小
- `element-plus` vendor chunk 仍有体积告警，当前不影响功能正确性

## 四、未完全覆盖项
- 更大范围浏览器回归
- 更复杂的数据权限自动化场景
- 外部通知通道验证
- 压测与并发验证
