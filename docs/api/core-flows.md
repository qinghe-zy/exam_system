# 核心接口流程说明

## 一、认证与权限
- `POST /api/auth/login`：登录获取 JWT
- `GET /api/auth/me`：获取当前用户
- `POST /api/auth/logout`：退出登录

## 二、系统管理
### 组织管理
- `GET /api/system/organizations`
- `POST /api/system/organizations`
- `PUT /api/system/organizations/{id}`
- `DELETE /api/system/organizations/{id}`

### 用户管理
- `GET /api/system/users`
- `POST /api/system/users`
- `PUT /api/system/users/{id}`
- `POST /api/system/users/import-candidates`

### 角色、菜单与审计
- `GET /api/system/roles`
- `GET /api/system/menus`
- `GET /api/system/menus/current`
- `GET /api/system/audit-logs`

### 配置中心
- `GET /api/system/config-center/configs`
- `POST /api/system/config-center/configs`
- `PUT /api/system/config-center/configs/{id}`
- `DELETE /api/system/config-center/configs/{id}`
- `GET /api/system/config-center/dictionaries`
- `POST /api/system/config-center/dictionaries`
- `PUT /api/system/config-center/dictionaries/{id}`
- `DELETE /api/system/config-center/dictionaries/{id}`

## 三、通知与消息
- `GET /api/notices`
- `GET /api/notices/{id}`
- `POST /api/notices`
- `PUT /api/notices/{id}`
- `DELETE /api/notices/{id}`
- `GET /api/messages/my`
- `POST /api/messages/{id}/read`

## 四、题库
- `GET /api/exam/questions`
- `GET /api/exam/questions/export`
- `POST /api/exam/questions/import`
- `POST /api/exam/questions`
- `PUT /api/exam/questions/{id}`
- `DELETE /api/exam/questions/{id}`
- `POST /api/exam/questions/ai/draft`
- `POST /api/exam/questions/ai/polish`

## 五、试卷与考试
- `GET /api/exam/papers`
- `GET /api/exam/papers/{id}`
- `POST /api/exam/papers`
- `PUT /api/exam/papers/{id}`
- `DELETE /api/exam/papers/{id}`
- `GET /api/exam/plans`
- `POST /api/exam/plans`
- `PUT /api/exam/plans/{id}`
- `DELETE /api/exam/plans/{id}`

## 六、考生端
- `GET /api/exam/candidate/my-exams`
- `GET /api/exam/candidate/exams/{examPlanId}?examPassword=...`
- `POST /api/exam/candidate/exams/{examPlanId}/save`
- `POST /api/exam/candidate/exams/{examPlanId}/submit`
- `POST /api/exam/candidate/exams/{examPlanId}/events`

说明：
- `startTime / endTime` 表示允许进入考试的时间窗口
- 工作区接口会返回 `entryDeadlineAt` 与 `answerDeadlineAt`
- 学生端展示的倒计时必须以 `answerDeadlineAt` 为准
- `events` 接口当前会上报切屏、窗口失焦、退出全屏等事件，并携带自动保存联动信息

## 七、阅卷、成绩与监考
- `GET /api/exam/grading/tasks`
- `GET /api/exam/grading/{answerSheetId}`
- `POST /api/exam/grading/{answerSheetId}/submit`
- `GET /api/exam/records`
- `GET /api/exam/analytics/overview`
- `GET /api/exam/proctor/events`

## 八、一致性说明
本文档已按当前 `controller` 层实际路径重新核对，能够对应到当前仓库中的真实接口实现。
