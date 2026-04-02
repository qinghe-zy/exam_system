# 发布检查清单

## 一、发布前必须检查
- 后端 compile/test/package 通过
- 前端 build 通过
- MySQL 空库重建导入通过
- 关键 API smoke 通过
- 文档已同步
- `.gitignore` 与敏感配置检查通过

## 二、远端检查
- `origin` 已正确配置
- 当前分支为 `main`
- 最新提交已推送到远端

## 三、交付检查
- 根目录正式文档可读、为中文主文档
- docs 子目录关键文档可读、为中文主文档
- HANDOFF 能清晰说明真实状态
