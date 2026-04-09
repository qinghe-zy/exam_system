# 考试核心模块说明

## 一、模块目标
考试核心模块负责把题库、试卷、考试发布、考生作答、阅卷评分、成绩分析、学生复盘和基础监考串成一条完整业务主链路。

## 二、模块组成
### 1. 题库管理
- 页面：`QuestionBankView`
- 接口：`/api/exam/questions*`
- 表：`biz_question_bank`
- 当前能力：题目维护、题目导入导出、知识点/标签/解析等元数据、填空题/论述题/材料题基础版、富文本 HTML、附件 JSON、知识点自动组题

### 2. 试卷与组卷
- 页面：`ExamPaperView`、`ExamPaperBuilderView`
- 接口：`/api/exam/papers*`
- 表：`biz_exam_paper`、`biz_paper_question`
- 当前能力：手工组卷、随机组卷、按题型/难度的基础策略组卷、试卷版本/备注/乱序配置、独立建卷页

### 3. 考试发布
- 页面：`ExamPlanView`
- 接口：`/api/exam/plans*`、`/api/notifications/exam-reminders/dispatch`
- 表：`biz_exam_plan`、`biz_exam_candidate`、`sys_notification_template`、`biz_notification_delivery_log`
- 当前通知模板已补 `organization_id`，支持组织模板覆盖全局模板。
- 当前能力：考试发布、考生分配、考试密码、迟到限制、提前交卷限制、参考次数限制基础版、开考前提醒扫描基础版、补考 / 缓考 / 重考基础版、原考试关联基础版、批次名称基础版、签到规则基础版、准考证 / 通知单基础版、考场 / 座位基础版、签到名单导出基础版

### 4. 考生端
- 页面：`CandidateExamView`、`CandidateScoreView`、`CandidateReviewCenterView`
- 接口：`/api/exam/candidate/*`、`/api/exam/records/my*`
- 表：`biz_answer_sheet`、`biz_answer_item`、`biz_score_record`
- 当前能力：
  - 待考列表、进入考试、进入窗口说明、实际作答倒计时
  - 待考列表可展示考试类型、批次和原考试信息
  - 待考列表可展示签到状态，并支持学生签到
  - 提供准考证 / 通知单基础版查看与打印
  - 可展示考场与座位信息
  - 严格考试态下的设备检测基础版（浏览器 / 窗口尺寸 / 移动端 / 全屏能力）
  - 固定答题卡、自动保存、手动保存、提交确认
  - 待复查标记与离页确认
  - 严格考试态下复制/粘贴/右键/高风险快捷键拦截
  - 已发布成绩查看、完整答卷回看、错题本基础版

### 5. 阅卷与成绩
- 页面：`GradingView`、`ExamRecordView`、`CandidateScoreView`
- 接口：`/api/exam/grading/*`、`/api/exam/records`、`/api/exam/records/my/*`、`/api/exam/score-appeals*`
- 表：`biz_grading_record`、`biz_score_record`、`biz_score_appeal`
- 当前能力：客观题自动判分、主观题人工评分、复核、退回重判、学生成绩申诉、管理端申诉处理、学生已发布成绩查看与详情展示

### 6. 分析与监考
- 页面：`AnalysisView`、`ProctorView`
- 接口：`/api/exam/analytics/overview`、`/api/exam/analytics/quality-report*`、`/api/exam/proctor/events`
- 表：`biz_anti_cheat_event`
- 当前能力：
  - 平均分、最高分、最低分、及格率、排名、分数段、知识点、题目得分率
  - 正式质量报告基础版：综合质量分、维度评分、风险提示、建议动作、分考试质量观察
  - 切屏、失焦、退出全屏、复制/粘贴/右键/快捷键拦截等事件查看
  - 客户端 IP、设备指纹、设备摘要留痕

### 7. 登录风险治理基础版
- 页面：`LoginRiskView`
- 接口：`/api/system/login-risks`
- 表：`biz_login_risk_log`
- 当前能力：记录成功/失败登录、基础风险级别、风险原因、IP 与设备信息，并支持管理员查看

### 8. 认证安全与运维基础版
- 页面：`LoginView`、`MessageCenterView`
- 接口：`/api/auth/login`、`/api/auth/verification-codes/send`
- 表：`sys_user`、`sys_config_item`、`biz_in_app_message`
- 当前能力：
  - 连续失败登录达到阈值后临时锁定账号
  - 同一客户端 IP 触发频率限制后阻断登录
  - 同一验证码目标具备冷却时间和窗口次数限制
  - 触发安全事件时向管理员和机构管理员发送 `SECURITY_ALERT`
  - 考试期间继续保护高风险更新 / 删除操作，但不阻断教师准备新的题目和新的试卷

### 9. 通知协同基础版
- 页面：`NoticeView`、`MessageCenterView`、`NotificationTemplateView`、`NotificationDeliveryLogView`
- 接口：`/api/notices*`、`/api/messages/*`、`/api/notifications/*`
- 表：`biz_in_app_message`、`sys_notification_template`、`biz_notification_delivery_log`
- 当前能力：
  - 公告管理、消息中心、通知模板管理
  - 考试发布、开考前提醒、成绩发布、申诉结果、安全告警统一通知分发
  - `IN_APP` 与 `MOCK_SMS` 两类投递通道
  - 投递日志查看与手动触发开考前提醒扫描
  - 公告与通知模板组织隔离基础版

### 9. AI 题库辅助
- 页面：`QuestionBankView`
- 接口：`/api/exam/questions/ai/*`
- 当前能力：生成题目草稿、优化题干/答案/解析（AI 辅助）

## 三、权限边界
- 管理员：可查看全局
- 组织管理员：按组织范围查看
- 教师：按组织范围查看考试业务数据
- 阅卷老师：访问阅卷相关数据
- 监考员：访问监考事件与风险留痕数据
- 学生：只访问自己的考试、消息、已发布成绩、答卷回看与错题本

## 四、当前限制
- 多设备登录限制与登录风险记录已实现基础版，但尚未覆盖完整登录态会话治理
- 更强设备检测、摄像头/麦克风等能力仍未实现
- 高级分析、更复杂规则引擎和真实外部通知网关仍未实现
