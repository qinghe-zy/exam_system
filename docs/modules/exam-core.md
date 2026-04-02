# 考试核心模块说明

## 一、模块目标
考试核心模块负责把题库、试卷、考试发布、考生作答、阅卷评分、成绩分析和基础监考串成一条完整业务主链路。

## 二、模块组成
### 1. 题库管理
- 页面：`QuestionBankView`
- 接口：`/api/exam/questions*`
- 表：`biz_question_bank`
- 当前能力：题目维护、题目导入导出、知识点/标签/解析等元数据

### 2. 试卷与组卷
- 页面：`ExamPaperView`
- 接口：`/api/exam/papers*`
- 表：`biz_exam_paper`、`biz_paper_question`
- 当前能力：手工组卷、随机组卷、基础策略组卷

### 3. 考试发布
- 页面：`ExamPlanView`
- 接口：`/api/exam/plans*`
- 表：`biz_exam_plan`、`biz_exam_candidate`
- 当前能力：考试发布、考生分配、考试密码、迟到限制、提前交卷限制、参考次数限制基础版

### 4. 考生端
- 页面：`CandidateExamView`
- 接口：`/api/exam/candidate/*`
- 表：`biz_answer_sheet`、`biz_answer_item`
- 当前能力：待考列表、进入考试、答题卡、自动保存、手动保存、交卷

### 5. 阅卷与成绩
- 页面：`GradingView`、`ExamRecordView`
- 接口：`/api/exam/grading/*`、`/api/exam/records`
- 表：`biz_grading_record`、`biz_score_record`
- 当前能力：客观题自动判分、主观题人工评分、成绩回写

### 6. 分析与监考
- 页面：`AnalysisView`、`ProctorView`
- 接口：`/api/exam/analytics/overview`、`/api/exam/proctor/events`
- 表：`biz_anti_cheat_event`
- 当前能力：平均分、最高分、最低分、及格率、排名、分数段、知识点、题目得分率、基础事件查看

## 三、权限边界
- 管理员：可查看全局
- 组织管理员：按组织范围查看
- 教师：按组织范围查看考试业务数据
- 阅卷老师：访问阅卷相关数据
- 学生：只访问自己的考试与消息

## 四、当前限制
- 数据权限仍是基础版
- 防作弊仍是基础版
- 高级分析和更复杂规则引擎仍未实现
