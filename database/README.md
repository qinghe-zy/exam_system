# 数据库目录说明

## 一、目录用途
本目录用于存放数据库侧的补充材料，而不是应用运行时直接加载的 SQL 资源。

## 二、建议放置内容
- 数据库迁移说明
- 回归验证记录
- 种子数据策略说明
- 回滚说明
- 结构调整评审记录

## 三、当前口径
- 可执行初始化 SQL 仍以 `sql/` 目录为准
- 当前正式交付初始化脚本为 `sql/mysql/init.sql`
- 当前运行时初始化脚本为：
  - `backend/src/main/resources/schema.sql`
  - `backend/src/main/resources/data.sql`
