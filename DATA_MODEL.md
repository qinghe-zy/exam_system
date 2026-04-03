# 数据模型说明

## 一、文档目的
本文档用于说明系统当前核心表结构、主链路关系、实现状态与数据库脚本口径，支撑数据库设计评审、代码实现核对和验收。

## 二、数据库口径
- 正式交付数据库：`exam_system`
- 全量初始化脚本：`sql/mysql/init.sql`
- 运行时初始化脚本：`backend/src/main/resources/schema.sql` 与 `backend/src/main/resources/data.sql`
- 当前无单独增量修复脚本；默认以全量重建方式回归验证

## 三、核心实体与表
### 1. 组织与权限
- `biz_organization`：组织树，支持学校、学院、班级、部门等层级
- `sys_role`：角色表
- `sys_user`：用户表，含组织、部门、考生编号等字段
- `sys_verification_code`：验证码表，用于注册和找回密码基础版
- `sys_menu`：菜单与权限可见性表

### 2. 题库与试卷
- `biz_question_bank`：题库表，包含题型、纯文本题干、富文本题干、材料内容、附件 JSON、选项 JSON、答案、解析、知识点、章节、标签、默认分值、审核状态、版本号
- `biz_exam_paper`：试卷表，除试卷基础信息外，新增试卷版本、备注、乱序开关、题型分布 JSON、难度分布 JSON
- `biz_paper_question`：试卷-题目关系表，存放题目顺序、分值、是否必答

### 3. 考试发布与作答
- `biz_exam_plan`：考试计划表
- `biz_exam_candidate`：考试-考生分配表
- `biz_answer_sheet`：答卷表
- `biz_answer_item`：答题项表

### 4. 阅卷与成绩
- `biz_grading_record`：阅卷记录
- `biz_score_record`：成绩记录

### 5. 运营与审计
- `biz_notice`：通知公告
- `biz_anti_cheat_event`：反作弊事件表，新增异常累计次数、是否联动自动保存、答卷版本字段
- `biz_audit_log`：审计日志表

## 四、主链路关系
1. `sys_user -> biz_exam_candidate -> biz_answer_sheet -> biz_answer_item -> biz_score_record`
2. `biz_question_bank -> biz_paper_question -> biz_exam_paper -> biz_exam_plan`
3. `biz_answer_sheet -> biz_grading_record`
4. `biz_answer_sheet -> biz_anti_cheat_event`

## 五、关键状态说明
### 1. 答卷状态
- `NOT_STARTED`：未开始
- `IN_PROGRESS`：作答中
- `SUBMITTED`：已提交，待阅卷
- `PARTIALLY_GRADED`：部分阅卷完成
- `GRADED`：已完成评分

### 2. 成绩状态
- `PENDING_GRADING`
- `PARTIALLY_GRADED`
- `PUBLISHED`

### 3. 考试时间语义
- `biz_exam_plan.start_time / end_time`：学生允许进入考试的时间窗口
- `biz_exam_plan.duration_minutes`：学生进入考试后的标准作答时长
- `biz_answer_sheet.started_at`：学生实际进入考试时间
- 实际作答截止时间：`min(started_at + duration_minutes, end_time)`

## 六、与实现一致性说明
当前代码中以下能力依赖这些表：
- 组织管理页依赖 `biz_organization`
- 用户管理与导入依赖 `sys_user`
- 题库页依赖 `biz_question_bank`
- 试卷工作台依赖 `biz_exam_paper` 与 `biz_paper_question`
- 考试计划页依赖 `biz_exam_plan` 与 `biz_exam_candidate`
- 考生工作区依赖 `biz_answer_sheet` 与 `biz_answer_item`
- 阅卷中心依赖 `biz_grading_record`
- 成绩中心与分析页依赖 `biz_score_record`
- 监考页依赖 `biz_anti_cheat_event`
- 审计页依赖 `biz_audit_log`

## 七、数据库回归验收要求
必须满足以下条件：
1. 删除并重建 `exam_system`
2. 从 `sql/mysql/init.sql` 完整导入
3. 核心表、角色、菜单、题型数据、组织种子数据存在
4. 系统以 MySQL 模式启动后关键接口可以读取数据

## 八、当前限制
- 当前未单独设计复杂索引策略说明文档，后续可继续细化
- 当前无独立迁移脚本链，主要依赖全量脚本回归
