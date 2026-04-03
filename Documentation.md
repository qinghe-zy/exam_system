# 开发记录

## 一、当前阶段
- 阶段名称：学生考试态修复 + 独立建卷页重构 + AI 题库辅助接入阶段
- 当前状态：代码、数据库、前后端入口与验证已完成，AI 外部调用受当前环境缺少私有密钥限制

## 二、已完成内容
### 1. 学生端考试页面修复
- 答题卡改为桌面端固定定位，主答题区滚动时右侧答题卡不再一起上滑。
- 全屏按钮改为双态切换：
  - 未全屏：显示“进入全屏”
  - 已全屏：显示“退出全屏”
- 主动点击“退出全屏”不再记为异常事件。
- 保存答案现在有真实接口调用与明确反馈。
- 提交试卷改为显式确认弹窗，并修复了考试态下弹窗被页面层盖住的问题。
- 考试态 overlay 层级已下调，关键弹窗统一 `append-to-body` 并抬高 `z-index`。

### 2. 管理端试卷创建与组卷重构
- 列表页与建卷页已拆分：
  - 列表页：`/exam/papers`
  - 新建页：`/exam/papers/create`
  - 编辑页：`/exam/papers/:paperId/edit`
- 旧“大而杂的单弹窗”不再作为主要建卷入口。
- 独立建卷页按流程拆分为：
  - 试卷基础信息
  - 手工选题
  - 随机组卷
  - 策略组卷
  - 卷面预览
- 已真实验证：
  - 手工选题建卷成功
  - 随机组卷建卷成功
  - 策略组卷建卷成功

### 3. 防作弊规则修正
- 当前仍保留异常检测：
  - 页面可见性变化
  - 窗口失焦
  - 全屏退出
  - 自动保存与异常事件联动
- 规则修正后：
  - 主动点击退出全屏：不记异常
  - 页面切换 / 标签切换 / 页面不可见：记异常
  - 异常触发后继续自动保存并落库
- 已通过真实验证：
  - 主动退出全屏前后 `FULLSCREEN_EXIT` 事件数不增加
  - 触发 `visibilitychange` 后 `TAB_SWITCH` 事件落库

### 4. AI 模块从预留推进到局部接入
- 已接入模块：题库管理
- 当前已落地能力：
  - AI 生成题目草稿
  - AI 优化当前题干 / 答案 / 解析 / 选项
- 前端入口：
  - 题库页顶部“AI 生成题目草稿”
  - 题目编辑弹窗内“AI 优化当前题目”
- 后端新增：
  - `QuestionAiController`
  - `AiQuestionAssistService`
  - `AiQuestionAssistServiceImpl`
  - `AiQuestionDraftRequest`
  - `AiQuestionPolishRequest`
  - `AiQuestionDraftVO`
  - `AiQuestionPolishVO`
  - `AiGatewayClient` 已升级为真实 DeepSeek HTTP 客户端
- 当前环境真实 blocker：
  - 本机未设置 `AI_API_KEY`
  - 因此已验证到“前端入口 -> 后端 controller -> service -> gateway -> 中文错误提示”闭环
  - 未能完成真实外部 DeepSeek 成功调用

## 三、本轮改动文件 / 模块
### 1. 前端
- `frontend/src/views/exam/CandidateExamView.vue`
- `frontend/src/views/exam/ExamPaperView.vue`
- `frontend/src/views/exam/ExamPaperBuilderView.vue`
- `frontend/src/views/exam/QuestionBankView.vue`
- `frontend/src/router/index.ts`
- `frontend/src/api/exam.ts`
- `frontend/src/types/exam.ts`

### 2. 后端
- `backend/src/main/java/com/projectexample/examsystem/infra/ai/AiGatewayClient.java`
- `backend/src/main/java/com/projectexample/examsystem/controller/QuestionAiController.java`
- `backend/src/main/java/com/projectexample/examsystem/service/AiQuestionAssistService.java`
- `backend/src/main/java/com/projectexample/examsystem/service/impl/AiQuestionAssistServiceImpl.java`
- AI 相关 DTO / VO
- `backend/src/main/resources/application.yml`

### 3. 文档
- `README.md`
- `Documentation.md`
- `EVALS.md`
- `HANDOFF.md`
- `docs/modules/paper-builder.md`
- `docs/modules/ai-question-assistant.md`
- `docs/modules/README.md`

## 四、验证结果
### 1. 构建 / 测试
- 后端 `mvn -q test`：通过
- 后端 `mvn -q -DskipTests compile`：通过
- 前端 `npm.cmd run build`：通过
- 前端 `npm.cmd run test:e2e`：通过（5 / 5）

### 2. 学生端真实验证
- 答题卡滚动前后位置：
  - `before.y = 219.18`
  - `after.y = 219.18`
  - 结论：桌面端已固定，不再跟随主内容上滑
- 全屏按钮：
  - 点击前：`进入全屏`
  - 点击后：`退出全屏`
  - 再点击：恢复 `进入全屏`
- 主动退出全屏：
  - `FULLSCREEN_EXIT` 事件数前后保持 0
- 切标签 / 页面不可见：
  - `TAB_SWITCH` 事件增加到 1
- 保存答案：
  - 页面出现“答案已保存”
- 提交试卷：
  - 出现“提交确认”弹窗，可关闭

### 3. 管理端建卷验证
- 手工卷已创建：`MAN-*`
- 随机卷已创建：`RND-*`
- 策略卷已创建：`STR-*`
- 三类试卷均已真实落库并可回查 `assemblyMode`

### 4. AI 链路验证
- 前端点击“AI 生成题目草稿”后，当前环境返回：
  - `AI 功能尚未配置。请在本地环境变量中设置 AI_API_KEY，并确认 AI_API_BASE_URL / AI_MODEL 可用后重试。`
- 后端直调 `/api/exam/questions/ai/draft` 也返回相同中文错误
- 结论：调用链已接通，真实外部成功调用受本地密钥缺失阻塞

## 五、剩余风险
- 当前 AI 仅接入题库模块，阅卷建议与成绩分析摘要尚未接入。
- 本机未配置 DeepSeek 私有密钥，AI 外部成功调用属于当前环境 blocker。
- 防作弊仍是浏览器侧基础实现，不含摄像头、人脸、设备锁定、多端协同。

## 六、下一步建议
1. 在本地私有环境变量中补齐 `AI_API_KEY` 后，直接验证 AI 草稿与 AI 优化成功路径。
2. 若进入下一阶段，优先扩展 AI 到阅卷评分建议。
3. 继续补真实人工考试态手测脚本，覆盖浏览器最小化、跨屏和网络波动。
