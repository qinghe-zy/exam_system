# 核心接口流程说明

## 一、登录与权限
### 登录
- 路径：`POST /api/auth/login`
- 用途：获取 JWT，供后续接口调用
- 返回：`token` 与当前用户信息

### 当前用户
- 路径：`GET /api/auth/me`
- 用途：获取当前登录用户身份信息

## 二、系统管理接口
### 组织管理
- `GET /api/system/organizations`：查询组织树
- `POST /api/system/organizations`：新增组织
- `PUT /api/system/organizations/{id}`：更新组织
- `DELETE /api/system/organizations/{id}`：删除组织

### 用户管理
- `GET /api/system/users`：查询用户列表
- `POST /api/system/users`：新增用户
- `PUT /api/system/users/{id}`：更新用户
- `POST /api/system/users/import-candidates`：批量导入考生

### 角色与菜单
- `GET /api/system/roles`
- `GET /api/system/menus`
- `GET /api/system/menus/current`

### 审计日志
- `GET /api/system/audit-logs`

## 三、题库接口
- `GET /api/exam/questions`
- `GET /api/exam/questions/export`
- `POST /api/exam/questions/import`
- `POST /api/exam/questions`
- `PUT /api/exam/questions/{id}`
- `DELETE /api/exam/questions/{id}`

## 四、试卷接口
- `GET /api/exam/papers`
- `GET /api/exam/papers/{id}`
- `POST /api/exam/papers`
- `PUT /api/exam/papers/{id}`
- `DELETE /api/exam/papers/{id}`

## 五、考试发布接口
- `GET /api/exam/plans`
- `POST /api/exam/plans`
- `PUT /api/exam/plans/{id}`
- `DELETE /api/exam/plans/{id}`

## 六、考生端接口
- `GET /api/exam/candidate/my-exams`
- `GET /api/exam/candidate/exams/{examPlanId}`
- `POST /api/exam/candidate/exams/{examPlanId}/save`
- `POST /api/exam/candidate/exams/{examPlanId}/submit`
- `POST /api/exam/candidate/exams/{examPlanId}/events`

## 七、阅卷与分析接口
- `GET /api/exam/grading/tasks`
- `GET /api/exam/grading/{answerSheetId}`
- `POST /api/exam/grading/{answerSheetId}/submit`
- `GET /api/exam/records`
- `GET /api/exam/analytics/overview`
- `GET /api/exam/proctor/events`

## 八、通知接口
- `GET /api/notices`
- `GET /api/notices/{id}`
- `POST /api/notices`
- `PUT /api/notices/{id}`
- `DELETE /api/notices/{id}`

## 九、与实现一致性说明
本文档中的接口路径已与当前 `controller` 层代码核对，当前版本可作为关键流程联调说明使用。
