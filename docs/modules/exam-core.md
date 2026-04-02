# 考试核心模块说明

## 一、模块目标
考试核心模块负责支撑从题目录入到成绩发布的完整业务主链路，是当前系统最核心的业务模块。

## 二、模块组成
### 1. 题库管理
- 页面：题库管理页
- 接口：`/api/exam/questions*`
- 表：`biz_question_bank`

### 2. 试卷管理
- 页面：试卷工作台
- 接口：`/api/exam/papers*`
- 表：`biz_exam_paper`、`biz_paper_question`

### 3. 考试发布
- 页面：考试计划页
- 接口：`/api/exam/plans*`
- 表：`biz_exam_plan`、`biz_exam_candidate`

### 4. 考生端
- 页面：待考与考试工作区
- 接口：`/api/exam/candidate/*`
- 表：`biz_answer_sheet`、`biz_answer_item`

### 5. 阅卷与成绩
- 页面：阅卷中心、成绩中心
- 接口：`/api/exam/grading/*`、`/api/exam/records`
- 表：`biz_grading_record`、`biz_score_record`

### 6. 分析与监考
- 页面：分析页、监考事件页
- 接口：`/api/exam/analytics/overview`、`/api/exam/proctor/events`
- 表：`biz_anti_cheat_event`

## 三、当前实现状态
- 已形成真实业务链路
- 部分能力仍是基础版，如复杂监考、严格数据权限、复杂策略组卷

## 四、验证方式
- 通过前后端构建
- 通过 MySQL 空库重建
- 通过关键接口 smoke
