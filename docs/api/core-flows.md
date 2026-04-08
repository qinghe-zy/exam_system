# 核心接口流程说明

## 一、认证与权限
- `POST /api/auth/login`：登录获取 JWT
- `GET /api/auth/register-options`：获取注册可选班级/部门
- `POST /api/auth/verification-codes/send`：发送注册 / 找回密码验证码（当前为 mock 通道）
- `POST /api/auth/register`：学生注册基础版
- `POST /api/auth/password/reset`：找回密码基础版
- `GET /api/auth/me`：获取当前用户
- `POST /api/auth/logout`：退出登录

说明：
- 登录接口当前已叠加：
  - 账号失败次数锁定
  - 客户端 IP 频率限制
  - 成功/失败登录风险留痕
- 验证码发送接口当前已叠加：
  - 同目标冷却时间限制
  - 同窗口发送次数限制

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
- `GET /api/system/login-risks`
- `GET /api/system/runtime/health`

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
- `POST /api/exam/questions/auto-group/knowledge-points`
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
- `GET /api/exam/records/my`
- `GET /api/exam/records/my/{id}`
- `GET /api/exam/records/my/wrong-book`

说明：
- `startTime / endTime` 表示允许进入考试的时间窗口
- 工作区接口会返回 `entryDeadlineAt` 与 `answerDeadlineAt`
- 学生端展示的倒计时必须以 `answerDeadlineAt` 为准
- 严格考试态下，工作区接口会同时返回设备检测策略，包括最小窗口尺寸、允许浏览器关键字、是否禁止移动端与是否要求全屏支持
- `save / submit` 当前除答案外，还会持久化学生对题目的“待复查”标记
- `events` 接口当前会上报切屏、窗口失焦、退出全屏等事件，并携带自动保存联动信息
- `events` 接口当前还会上报复制/粘贴/右键/快捷键拦截与设备上下文，并由服务端补写客户端 IP
- 严格考试态下，若同一考试同一账号已记录其他设备指纹，再从另一设备进入会被拒绝
- `wrong-book` 会按学生已发布成绩动态聚合错题

## 七、阅卷、成绩与监考
- `GET /api/exam/grading/tasks`
- `GET /api/exam/grading/{answerSheetId}`
- `POST /api/exam/grading/{answerSheetId}/submit`
- `POST /api/exam/grading/{answerSheetId}/review`
- `GET /api/exam/records`
- `GET /api/exam/analytics/overview`
- `GET /api/exam/analytics/quality-report`
- `GET /api/exam/analytics/quality-report/export`
- `GET /api/exam/proctor/events`
- `GET /api/exam/score-appeals`
- `GET /api/exam/score-appeals/my/{scoreRecordId}`
- `POST /api/exam/score-appeals/my/{scoreRecordId}`
- `POST /api/exam/score-appeals/{appealId}/process`

说明：
- `GET /api/exam/proctor/events` 当前会返回事件类型、严重级别、自动保存状态、客户端 IP、设备指纹和设备摘要
- 阅卷提交后，主观题答卷当前会先进入 `REVIEW_PENDING`，再由复核动作决定是否发布
- 学生申诉只允许针对已发布成绩提交，且同一成绩仅允许存在一条处理中申诉
- 质量报告接口当前会返回综合质量分、风险提示、建议动作、分考试结论和薄弱知识点/题目

## 八、一致性说明
本文档已按当前 `controller` 层实际路径重新核对，能够对应到当前仓库中的真实接口实现。
