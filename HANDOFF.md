# 交接说明

## 1. 仓库状态

- 当前分支目标：`main`
- 当前远端：`https://github.com/qinghe-zy/exam_system.git`
- 当前仓库已完成代码、截图、图示和正式文档同步

## 2. 建议接手顺序

1. 阅读 `README.md`
2. 阅读 `docs/ops/产品级用户使用说明书.md`
3. 按说明启动前后端
4. 执行 `scripts/verify-mysql-init.ps1`
5. 执行 `npx.cmd playwright test`

## 3. 当前重点资源

- 页面截图：`docs/assets/screenshots/`
- 流程与架构图：`docs/assets/diagrams/`
- 模块说明：`docs/modules/exam-core.md`
- 运维 runbook：`docs/runbooks/`

## 4. 当前运行口径

- 前端默认地址：`http://127.0.0.1:5173`
- 后端默认地址：`http://127.0.0.1:8083`
- Swagger：`http://127.0.0.1:8083/swagger-ui.html`

## 5. 关键验证命令

```powershell
cd backend
mvn -q test
mvn -q -DskipTests package

cd ..\\frontend
npm.cmd run build
npx.cmd playwright test

cd ..
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/verify-mysql-init.ps1
```

## 6. 当前已知限制

- 真实短信 / 邮件 / 企业微信 / 钉钉未接入
- 高级防作弊能力未接入
- 趋势分析和组织对比仍可继续深化

## 7. 接手注意事项

- 若运行 `mvn clean` 或 `mvn package` 失败，请先确认后端 `java` 进程未占用 `backend/target/*.jar`
- 若需要 MySQL 口径，请使用环境变量启用 `mysql` profile
- 若继续生成图示，请执行 `node scripts/generate-drawio-diagrams.mjs` 后再调用 draw.io CLI 导出
