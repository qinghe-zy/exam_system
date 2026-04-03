# AI 题库辅助模块说明

## 一、目标
将 AI 能力从“仅预留”推进到题库模块中的最小闭环，先服务教师出题与题库维护。

## 二、当前接入场景
### 1. AI 生成题目草稿
- 入口：题库页顶部“AI 生成题目草稿”
- 用途：按学科、题型、难度、知识点生成候选题目草稿
- 输出：题干、选项、答案、解析、知识点、章节、标签、默认分值

### 2. AI 优化当前题目
- 入口：题目编辑弹窗内“AI 优化当前题目”
- 用途：对当前题干、答案、解析和选项进行润色优化
- 输出：优化后的题干、答案、解析、选项建议

## 三、后端实现位置
- 控制器：
  - `backend/src/main/java/com/projectexample/examsystem/controller/QuestionAiController.java`
- 服务：
  - `backend/src/main/java/com/projectexample/examsystem/service/AiQuestionAssistService.java`
  - `backend/src/main/java/com/projectexample/examsystem/service/impl/AiQuestionAssistServiceImpl.java`
- DTO：
  - `AiQuestionDraftRequest.java`
  - `AiQuestionPolishRequest.java`
- VO：
  - `AiQuestionDraftVO.java`
  - `AiQuestionPolishVO.java`
- 网关：
  - `backend/src/main/java/com/projectexample/examsystem/infra/ai/AiGatewayClient.java`
  - `backend/src/main/java/com/projectexample/examsystem/infra/ai/AiGatewayProperties.java`

## 四、DeepSeek 配置方式
### 1. 建议本地环境变量
- `AI_API_BASE_URL=https://api.deepseek.com`
- `AI_API_KEY=本地私有密钥`
- `AI_MODEL=deepseek-chat`

### 2. 仓库内保留的仅是占位
- `.env.example`
- `backend/src/main/resources/application.yml`

### 3. 禁止提交真实密钥
- `.env`
- `.env.local`
- `frontend/.env.local`
- `backend/src/main/resources/application-local.yml`
- 任何仓库内可跟踪的环境配置文件

## 五、当前真实状态
- 本地环境未检测到 `AI_API_KEY`
- 因此当前 AI 能力已具备真实调用链，但在本机只能验证到：
  - 前端入口存在
  - 后端接口存在
  - 配置缺失时返回明确中文错误
- 一旦本地补齐 DeepSeek 私有密钥，即可直接切换到真实外部调用

## 六、提示语原则
- 所有 AI 输出均标注为“AI 辅助”
- AI 结果不能直接替代教师最终结论
- 调用失败时不静默，必须返回可理解的中文提示
