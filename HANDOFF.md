# 交接说明

## 一、当前真实状态
当前仓库已完成：
- 教师端试卷工作台重构
- 教师端新建考试弹窗重构
- 学生端考试工作区沉浸式改版
- 倒计时语义修复
- 基础防作弊检测、自动保存联动、事件落库、后台可查闭环
- MySQL 结构同步、空库重建回归、浏览器级 E2E 回归

## 二、已完全实现
- 新建试卷支持：
  - 题库筛选
  - 手工取题
  - 随机组卷
  - 按题型/难度策略组卷
  - 分值、顺序、必答/选答设置
  - 试卷版本、备注、乱序、卷面说明保存
- 新建考试支持：
  - 试卷摘要展示
  - 开始/结束时间与入场窗口语义说明
  - 考试时长、及格线、参考次数、口令、防作弊等级、自动交卷、考生名单设置
- 学生端支持：
  - 允许进入窗口展示
  - 本场剩余作答时间倒计时
  - 自动保存、手动保存、提交试卷
  - 全屏入口、固定答题卡、当前题目聚焦
- 防作弊基础版支持：
  - 页面切换检测
  - 窗口失焦检测
  - 全屏退出检测
  - 异常行为触发自动保存
  - 事件写入 `biz_anti_cheat_event`
  - 管理员/监考端查看事件

## 三、数据库做了什么
- `biz_exam_paper` 新增：
  - `paper_version`
  - `remark_text`
  - `shuffle_enabled`
  - `question_type_config_json`
  - `difficulty_config_json`
- `biz_anti_cheat_event` 新增：
  - `leave_count`
  - `triggered_auto_save`
  - `save_version`
- 已重建 `exam_system` 并导入最新 `sql/mysql/init.sql`

## 四、AI 模块核查结果
- 当前结论：仅做接入预留，未接入真实 AI 能力
- 代码位置：
  - `.env.example`
  - `backend/src/main/resources/application.yml`
  - `backend/src/main/java/com/projectexample/examsystem/infra/ai/AiGatewayProperties.java`
  - `backend/src/main/java/com/projectexample/examsystem/infra/ai/AiGatewayClient.java`
- 当前未发现：
  - AI 前端入口
  - AI controller / service 业务实现
  - 真实模型调用

## 五、验证跑了什么
- 后端 `mvn -q test`
- 后端 `mvn -q -DskipTests package`
- 前端 `npm.cmd run build`
- 前端 `npm.cmd run test:e2e`
- MySQL 空库重建
- MySQL 模式接口 smoke
- 教师策略组卷保存试卷
- 教师创建测试考试
- 学生进入考试并显示正确倒计时
- 防作弊事件落库与后台查询

## 六、当前剩余问题
- `element-plus` vendor chunk 仍有体积告警
- 基础防作弊仅覆盖浏览器级异常留痕，不包含设备锁定/摄像头/人脸识别
- 数据权限仍是基础组织范围约束

## 七、特别说明
- 本轮未覆盖用户已有改动：`frontend/tests/e2e/grading-flow.spec.ts`
- 远端 `origin` 已正确指向 `https://github.com/qinghe-zy/exam_system.git`

## 八、后续建议
1. 补更严格的数据权限与监考权限矩阵。
2. 补真实人工手测脚本，覆盖切屏、窗口最小化、全屏退出。
3. 若进入下一阶段，再扩展 AI、通知、外部监考能力。
